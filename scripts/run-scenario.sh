#!/usr/bin/env bash
set -euo pipefail

command -v curl >/dev/null 2>&1 || { echo "curl is required on PATH" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "jq is required on PATH" >&2; exit 1; }

CURL_BIN=${CURL_BIN:-curl}
read -r -a CURL_CMD <<< "${CURL_BIN}"
if [[ ${#CURL_CMD[@]} -eq 0 ]]; then
  echo "CURL_BIN is empty" >&2
  exit 1
fi

run_curl() {
  "${CURL_CMD[@]}" "$@"
}

BASE_HOST=${BASE_HOST:-localhost}
GATEWAY_URL=${GATEWAY_URL:-http://${BASE_HOST}:9000}
USERS_URL=${USERS_URL:-http://${BASE_HOST}:8081}
ARTICLES_URL=${ARTICLES_URL:-http://${BASE_HOST}:8082}
ORDERS_URL=${ORDERS_URL:-http://${BASE_HOST}:8083}
NOTIFICATIONS_URL=${NOTIFICATIONS_URL:-http://${BASE_HOST}:8084}

ADMIN_USERNAME=${ADMIN_USERNAME:-admin}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@example.com}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-Admin123!}

USER_USERNAME=${USER_USERNAME:-alice}
USER_EMAIL=${USER_EMAIL:-alice@example.com}
USER_PASSWORD=${USER_PASSWORD:-Alice123!}

ARTICLE_NAME_DEFAULT="Scenario Article $(date +%s)"
ARTICLE_NAME=${ARTICLE_NAME:-${ARTICLE_NAME_DEFAULT}}
ARTICLE_DESCRIPTION=${ARTICLE_DESCRIPTION:-"Created via run-scenario.sh"}
ARTICLE_PRICE=${ARTICLE_PRICE:-199.99}
ARTICLE_INITIAL_STOCK=${ARTICLE_INITIAL_STOCK:-20}
ARTICLE_CATEGORY=${ARTICLE_CATEGORY:-"Scenario"}

WAIT_ATTEMPTS=${WAIT_ATTEMPTS:-60}
WAIT_DELAY_SECONDS=${WAIT_DELAY_SECONDS:-2}
NOTIFICATION_ATTEMPTS=${NOTIFICATION_ATTEMPTS:-10}
NOTIFICATION_DELAY_SECONDS=${NOTIFICATION_DELAY_SECONDS:-3}

ADMIN_TOKEN=""
ADMIN_ID=""
USER_TOKEN=""
USER_ID=""
ARTICLE_ID=""
ORDER_ID=""
ORDER_ID_TO_CANCEL=""

log_step() {
  echo ""
  echo "== $1 =="
}

wait_for_service() {
  local name=$1
  local url=$2
  local attempts=${3:-$WAIT_ATTEMPTS}
  local delay=${4:-$WAIT_DELAY_SECONDS}

  echo ""
  echo "Waiting for ${name} (${url}) ..."
  for ((i=1; i<=attempts; i++)); do
    local tmp status exit_code=0
    tmp=$(mktemp)
    status=$(run_curl -s -o "$tmp" -w '%{http_code}' "$url" 2>/dev/null) || exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
      if [[ "$status" == "200" ]] && jq -e '.status == "UP"' "$tmp" >/dev/null 2>&1; then
        echo "  -> ${name} is ready"
        rm -f "$tmp"
        return 0
      fi

      case "$status" in
        200|204)
          echo "  -> ${name} responded (HTTP ${status})"
          rm -f "$tmp"
          return 0
          ;;
        401|403|404|500)
          echo "  -> ${name} reachable (HTTP ${status}); assuming ready"
          rm -f "$tmp"
          return 0
          ;;
      esac
    fi

    rm -f "$tmp"
    sleep "$delay"
  done

  echo "  -> timeout waiting for ${name}" >&2
  exit 1
}

register_account() {
  local role=$1
  local username=$2
  local email=$3
  local password=$4

  log_step "Register ${role} account (${username})"

  local payload
  payload=$(jq -n \
    --arg username "$username" \
    --arg email "$email" \
    --arg password "$password" \
    '{username:$username,email:$email,password:$password}')

  local endpoint="${USERS_URL}/api/auth/register"
  if [[ "$role" == "ADMIN" ]]; then
    endpoint="${USERS_URL}/api/auth/register/admin"
  fi

  local tmp status
  tmp=$(mktemp)
  status=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "${endpoint}")

  case "$status" in
    200|201)
      echo "  -> account created"
      ;;
    409)
      echo "  -> account already exists"
      ;;
    *)
      echo "Registration failed with status ${status}" >&2
      cat "$tmp" >&2
      rm -f "$tmp"
      exit 1
      ;;
  esac

  rm -f "$tmp"
}

login_account() {
  local username=$1
  local password=$2
  local token_var=$3

  log_step "Authenticate ${username}"

  local payload
  payload=$(jq -n \
    --arg username "$username" \
    --arg password "$password" \
    '{username:$username,password:$password}')

  local response token
  response=$(run_curl -sS \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "${USERS_URL}/api/auth/login")

  token=$(echo "$response" | jq -r '.token // empty')
  if [[ -z "$token" ]]; then
    echo "Failed to obtain token for ${username}" >&2
    echo "$response" >&2
    exit 1
  fi

  printf -v "$token_var" '%s' "$token"
  echo "  -> token acquired"
}

resolve_user_id() {
  local token=$1
  local label=$2
  local response

  response=$(run_curl -sS \
    -H "Authorization: Bearer ${token}" \
    "${USERS_URL}/api/users/me")

  local id
  id=$(echo "$response" | jq -r '.id // empty')

  if [[ -z "$id" ]]; then
    echo "Unable to resolve user id for ${label}" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> ${label} id: ${id}"
  echo "$id"
}

create_article() {
  log_step "Admin creates article '${ARTICLE_NAME}'"

  local payload
  payload=$(jq -n \
    --arg name "$ARTICLE_NAME" \
    --arg description "$ARTICLE_DESCRIPTION" \
    --argjson price "$ARTICLE_PRICE" \
    --argjson stock "$ARTICLE_INITIAL_STOCK" \
    --arg category "$ARTICLE_CATEGORY" \
    '{name:$name,description:$description,price:$price,stock:$stock,category:$category}')

  local response article_id
  response=$(run_curl -sS \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "${ARTICLES_URL}/api/articles")

  article_id=$(echo "$response" | jq -r '.id // empty')
  if [[ -z "$article_id" ]]; then
    echo "Failed to create article" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> article id: ${article_id}"
  ARTICLE_ID="$article_id"
}

user_browse_articles() {
  log_step "User lists articles"
  local response
  response=$(run_curl -sS "${ARTICLES_URL}/api/articles")

  if ! echo "$response" | jq -e --argjson id "$ARTICLE_ID" '.[] | select(.id == $id)' >/dev/null 2>&1; then
    echo "Created article ${ARTICLE_ID} not present in listing" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> article ${ARTICLE_ID} visible in catalogue"
}

user_view_article() {
  log_step "User views article ${ARTICLE_ID}"
  local response
  response=$(run_curl -sS "${ARTICLES_URL}/api/articles/${ARTICLE_ID}")

  local id
  id=$(echo "$response" | jq -r '.id // empty')

  if [[ "$id" != "$ARTICLE_ID" ]]; then
    echo "Unexpected response when fetching article ${ARTICLE_ID}" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> article details retrieved"
}

user_create_order() {
  local target_var=${1:-ORDER_ID}

  log_step "User creates order for article ${ARTICLE_ID}"

  local payload
  payload=$(jq -n \
    --argjson articleId "$ARTICLE_ID" \
    '{items:[{articleId:$articleId,quantity:1}]}')

  local response order_id
  response=$(run_curl -sS \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "${ORDERS_URL}/api/orders")

  order_id=$(echo "$response" | jq -r '.id // empty')
  if [[ -z "$order_id" ]]; then
    echo "Failed to create order" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> order id: ${order_id}"
  printf -v "$target_var" "%s" "$order_id"
  if [[ "$target_var" == "ORDER_ID" ]]; then
    ORDER_ID="$order_id"
  fi
}

wait_for_notification() {
  local token=$1
  local expected_type=$2
  local related_id=$3
  local label=$4
  local attempts=${5:-$NOTIFICATION_ATTEMPTS}
  local delay=${6:-$NOTIFICATION_DELAY_SECONDS}

  echo ""
  echo "Waiting for ${label} notification (${expected_type}) ..."

  for ((i=1; i<=attempts; i++)); do
    local response
    response=$(run_curl -sS \
      -H "Authorization: Bearer ${token}" \
      "${NOTIFICATIONS_URL}/api/notifications")

    if echo "$response" | jq -e --arg type "$expected_type" --argjson rel "$related_id" \
      '.[] | select(.type == $type) | select((.relatedEntityId // -1) == $rel)' >/dev/null 2>&1; then
      echo "  -> notification received"
      return 0
    fi

    sleep "$delay"
  done

  echo "  -> notification not observed"
  return 1
}

user_update_order_status() {
  local order_id=$1
  local new_status=$2

  log_step "User updates order ${order_id} status to ${new_status}"

  local tmp status_code
  tmp=$(mktemp)
  status_code=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -X PATCH \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${ORDERS_URL}/api/orders/${order_id}/status?status=${new_status}")

  if [[ "$status_code" != "200" ]]; then
    echo "Failed to update order ${order_id} status to ${new_status} (status ${status_code})" >&2
    cat "$tmp" >&2
    rm -f "$tmp"
    exit 1
  fi

  rm -f "$tmp"
  echo "  -> status updated"
}

user_cancel_order() {
  local order_id=$1

  log_step "User cancels order ${order_id}"

  local tmp status_code
  tmp=$(mktemp)
  status_code=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -X DELETE \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${ORDERS_URL}/api/orders/${order_id}")

  if [[ "$status_code" != "204" ]]; then
    echo "Failed to cancel order ${order_id} (status ${status_code})" >&2
    cat "$tmp" >&2
    rm -f "$tmp"
    exit 1
  fi

  rm -f "$tmp"
  echo "  -> order cancelled"
}

user_assert_order_in_list() {
  local order_id=$1
  local expected_status=${2:-}

  log_step "User verifies order ${order_id} in order list"

  local response
  response=$(run_curl -sS \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${ORDERS_URL}/api/orders")

  local status
  status=$(echo "$response" | jq -r --argjson id "$order_id" \
    '.[] | select(.id == $id) | .status // empty')

  if [[ -z "$status" ]]; then
    echo "Order ${order_id} not present in user list" >&2
    echo "$response" >&2
    exit 1
  fi

  if [[ -n "$expected_status" && "$status" != "$expected_status" ]]; then
    echo "Order ${order_id} expected status ${expected_status} but was ${status}" >&2
    exit 1
  fi

  echo "  -> order ${order_id} listed with status ${status}"
}

user_get_order() {
  local order_id=$1
  local expected_status=${2:-}

  log_step "User retrieves order ${order_id} details"

  local response
  response=$(run_curl -sS \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${ORDERS_URL}/api/orders/${order_id}")

  local id status
  id=$(echo "$response" | jq -r '.id // empty')
  status=$(echo "$response" | jq -r '.status // empty')

  if [[ "$id" != "$order_id" ]]; then
    echo "Unexpected response when fetching order ${order_id}" >&2
    echo "$response" >&2
    exit 1
  fi

  if [[ -n "$expected_status" && "$status" != "$expected_status" ]]; then
    echo "Order ${order_id} expected status ${expected_status} but was ${status}" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "  -> order status: ${status}"
}

user_get_unread_count() {
  local response count
  response=$(run_curl -sS \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${NOTIFICATIONS_URL}/api/notifications/unread/count")

  count=$(echo "$response" | jq -r '.unreadCount // empty')
  if ! [[ "$count" =~ ^[0-9]+$ ]]; then
    echo "Unable to read unread notification count" >&2
    echo "$response" >&2
    exit 1
  fi

  echo "$count"
}

user_mark_first_notification_read() {
  log_step "User marks first unread notification as read"

  local list unread_id tmp status
  list=$(run_curl -sS \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${NOTIFICATIONS_URL}/api/notifications")

  unread_id=$(echo "$list" | jq -r '[.[] | select(.isRead == false)][0].id // empty')

  if [[ -z "$unread_id" ]]; then
    echo "  -> no unread notifications to mark"
    return 0
  fi

  tmp=$(mktemp)
  status=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -X PATCH \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${NOTIFICATIONS_URL}/api/notifications/${unread_id}/read")

  if [[ "$status" != "200" ]]; then
    echo "Failed to mark notification ${unread_id} as read (status ${status})" >&2
    cat "$tmp" >&2
    rm -f "$tmp"
    exit 1
  fi

  rm -f "$tmp"
  echo "  -> notification ${unread_id} marked as read"
}

user_mark_all_notifications_read() {
  log_step "User marks all notifications as read"

  local tmp status
  tmp=$(mktemp)
  status=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -X PATCH \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    "${NOTIFICATIONS_URL}/api/notifications/read-all")

  if [[ "$status" != "200" ]]; then
    echo "Failed to mark all notifications as read (status ${status})" >&2
    cat "$tmp" >&2
    rm -f "$tmp"
    exit 1
  fi

  rm -f "$tmp"
  echo "  -> all notifications marked as read"
}

mark_article_unavailable() {
  log_step "Admin marks article ${ARTICLE_ID} as unavailable (stock = 0)"

  local payload
  payload=$(jq -n --argjson stock 0 '{stock:$stock}')

  local tmp status
  tmp=$(mktemp)
  status=$(run_curl -sS -o "$tmp" -w '%{http_code}' \
    -X PATCH \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "${ARTICLES_URL}/api/articles/${ARTICLE_ID}/stock")

  if [[ "$status" != "200" ]]; then
    echo "Failed to update stock (status ${status})" >&2
    cat "$tmp" >&2
    rm -f "$tmp"
    exit 1
  fi

  rm -f "$tmp"
  echo "  -> stock updated to zero"
}

assert_article_unavailable() {
  log_step "Validate article ${ARTICLE_ID} availability status"

  local response stock
  response=$(run_curl -sS "${ARTICLES_URL}/api/articles/${ARTICLE_ID}")
  stock=$(echo "$response" | jq -r '.stock // empty')

  if [[ "$stock" != "0" ]]; then
    echo "Article ${ARTICLE_ID} stock is expected to be 0 but is ${stock}" >&2
    echo "$response" >&2
    exit 1
  fi

  local available
  available=$(run_curl -sS "${ARTICLES_URL}/api/articles/available")
  if echo "$available" | jq -e --argjson id "$ARTICLE_ID" '.[] | select(.id == $id)' >/dev/null 2>&1; then
    echo "Article ${ARTICLE_ID} still appears in available list" >&2
    echo "$available" >&2
    exit 1
  fi

  echo "  -> article no longer listed as available"
}

main() {
  wait_for_service "API Gateway" "${GATEWAY_URL}/q/health/ready"
  wait_for_service "Users service" "${USERS_URL}/q/health/ready"
  wait_for_service "Articles service" "${ARTICLES_URL}/q/health/ready"
  wait_for_service "Orders service" "${ORDERS_URL}/q/health/ready"
  wait_for_service "Notifications service" "${NOTIFICATIONS_URL}/q/health/ready"

  register_account "ADMIN" "$ADMIN_USERNAME" "$ADMIN_EMAIL" "$ADMIN_PASSWORD"
  login_account "$ADMIN_USERNAME" "$ADMIN_PASSWORD" ADMIN_TOKEN
  ADMIN_ID=$(resolve_user_id "$ADMIN_TOKEN" "admin")

  register_account "USER" "$USER_USERNAME" "$USER_EMAIL" "$USER_PASSWORD"
  login_account "$USER_USERNAME" "$USER_PASSWORD" USER_TOKEN
  USER_ID=$(resolve_user_id "$USER_TOKEN" "user")

  create_article
  user_browse_articles
  user_view_article
  user_create_order

  user_assert_order_in_list "$ORDER_ID" "PENDING"
  user_get_order "$ORDER_ID" "PENDING"

  wait_for_notification "$USER_TOKEN" "ORDER_CREATED" "$ORDER_ID" "user order-created" || {
    echo "User did not receive ORDER_CREATED notification within timeout" >&2
    exit 1
  }

  user_update_order_status "$ORDER_ID" "CONFIRMED"
  user_get_order "$ORDER_ID" "CONFIRMED"
  user_assert_order_in_list "$ORDER_ID" "CONFIRMED"
  wait_for_notification "$USER_TOKEN" "ORDER_CONFIRMED" "$ORDER_ID" "user order-confirmed" || {
    echo "User did not receive ORDER_CONFIRMED notification within timeout" >&2
    exit 1
  }

  user_update_order_status "$ORDER_ID" "SHIPPED"
  user_get_order "$ORDER_ID" "SHIPPED"
  user_assert_order_in_list "$ORDER_ID" "SHIPPED"
  wait_for_notification "$USER_TOKEN" "ORDER_SHIPPED" "$ORDER_ID" "user order-shipped" || {
    echo "User did not receive ORDER_SHIPPED notification within timeout" >&2
    exit 1
  }

  user_update_order_status "$ORDER_ID" "DELIVERED"
  user_get_order "$ORDER_ID" "DELIVERED"
  user_assert_order_in_list "$ORDER_ID" "DELIVERED"
  wait_for_notification "$USER_TOKEN" "ORDER_DELIVERED" "$ORDER_ID" "user order-delivered" || {
    echo "User did not receive ORDER_DELIVERED notification within timeout" >&2
    exit 1
  }

  user_create_order ORDER_ID_TO_CANCEL
  user_assert_order_in_list "$ORDER_ID_TO_CANCEL" "PENDING"
  user_get_order "$ORDER_ID_TO_CANCEL" "PENDING"
  wait_for_notification "$USER_TOKEN" "ORDER_CREATED" "$ORDER_ID_TO_CANCEL" "user order-created (cancelled order)" || {
    echo "User did not receive ORDER_CREATED notification for cancellable order within timeout" >&2
    exit 1
  }

  user_cancel_order "$ORDER_ID_TO_CANCEL"
  user_get_order "$ORDER_ID_TO_CANCEL" "CANCELLED"
  user_assert_order_in_list "$ORDER_ID_TO_CANCEL" "CANCELLED"
  wait_for_notification "$USER_TOKEN" "ORDER_CANCELLED" "$ORDER_ID_TO_CANCEL" "user order-cancelled" || {
    echo "User did not receive ORDER_CANCELLED notification within timeout" >&2
    exit 1
  }

  user_get_order "$ORDER_ID" "DELIVERED"
  user_assert_order_in_list "$ORDER_ID" "DELIVERED"

  unread_before=$(user_get_unread_count)
  echo "  -> user has ${unread_before} unread notifications"
  user_mark_first_notification_read
  unread_after_first=$(user_get_unread_count)
  if [[ "$unread_before" -gt 0 && "$unread_after_first" -ge "$unread_before" ]]; then
    echo "Unread count did not decrease after marking a notification" >&2
    exit 1
  fi
  user_mark_all_notifications_read
  unread_final=$(user_get_unread_count)
  if [[ "$unread_final" != "0" ]]; then
    echo "User still has ${unread_final} unread notifications after mark-all" >&2
    exit 1
  fi

  mark_article_unavailable
  assert_article_unavailable

  if [[ "$ADMIN_ID" == "1" ]]; then
    wait_for_notification "$ADMIN_TOKEN" "STOCK_LOW" "$ARTICLE_ID" "admin stock-low" || {
      echo "Admin did not receive STOCK_LOW notification within timeout" >&2
      exit 1
    }
  else
    echo ""
    echo "Note: Admin user id is ${ADMIN_ID}. Notifications service currently targets userId=1 for STOCK_LOW alerts."
    echo "      Low stock notification verification skipped."
  fi

  echo ""
  echo "Scenario completed successfully:"
  echo "  - Admin: ${ADMIN_USERNAME} (id ${ADMIN_ID})"
  echo "  - User: ${USER_USERNAME} (id ${USER_ID})"
  echo "  - Article id: ${ARTICLE_ID}"
  echo "  - Order id: ${ORDER_ID}"
}

main "$@"
