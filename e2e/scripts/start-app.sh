#!/usr/bin/env bash
#
# Copyright 2026 agwlvssainokuni
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Starts the MasterMeister5 backend (WAR incl. bundled frontend) for E2E
# testing, with a throwaway internal H2 database recreated on every run so
# the test suite is idempotent regardless of what a previous run left behind.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

rm -rf "$REPO_ROOT/e2e/.data"
mkdir -p "$REPO_ROOT/e2e/.data"

export MM5_INTERNAL_DB_PATH="$REPO_ROOT/e2e/.data/mastermeister5"
export MM5_JWT_SECRET="e2e-test-jwt-secret-not-for-production-use-0001"
export MM5_CONNECTION_SECRET_KEY="e2e-test-connection-secret-not-for-prod-0001"
export MM5_INITIAL_ADMIN_EMAIL="e2e-admin@example.com"
export MM5_INITIAL_ADMIN_PASSWORD="E2eAdminPass123!"

exec ./gradlew :backend:bootRun --console=plain
