#!/bin/bash
# Run sonarqube
# Usage: ./sonarscan.sh [--autostop]

AUTOSTOP=false
if [ "$1" = "--autostop" ]; then
    AUTOSTOP=true
fi

docker network create sonarnet
docker run -d --name sonarqube --network sonarnet -p 9000:9000 sonarqube:lts

echo "Waiting for sonarqube to start..."

sleep 5

# Wait for the sonarqube container to start (use the api to tell when it is ready)
while [ "$(curl -s http://localhost:9000/api/system/status | jq -r .status)" != "UP" ]; do
    sleep 5
done

echo "Analyzing code..."

docker run --rm \
  --network sonarnet \
  -v "$(pwd)/../app/src/main/java:/usr/src" \
  sonarsource/sonar-scanner-cli \
  -Dsonar.projectKey=trail-sense \
  -Dsonar.sources=/usr/src \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.exclusions=**/*.java \
  -Dsonar.issue.ignore.multicriteria=e1 \
  -Dsonar.issue.ignore.multicriteria.e1.ruleKey=kotlin:S117 \
  -Dsonar.issue.ignore.multicriteria.e1.resourceKey=**/AppDatabase.kt

echo ""
echo "Waiting for analysis to be processed..."

# Wait for the background task to complete
while [ "$(curl -s -u admin:admin http://localhost:9000/api/ce/activity_status | jq -r .pending)" != "0" ] || \
      [ "$(curl -s -u admin:admin http://localhost:9000/api/ce/activity_status | jq -r .inProgress)" != "0" ]; do
    sleep 2
done

# Export issues to file
echo "Exporting issues..."
PAGE=1
PAGE_SIZE=500
ISSUES_FILE="$(pwd)/../sonarqube-issues.json"
REPORT_FILE="$(pwd)/../sonarqube-report.json"
ALL_ISSUES="[]"

while true; do
    RESPONSE=$(curl -s -u admin:admin "http://localhost:9000/api/issues/search?componentKeys=trail-sense&ps=$PAGE_SIZE&p=$PAGE")
    ISSUES=$(echo "$RESPONSE" | jq '.issues')
    COUNT=$(echo "$ISSUES" | jq 'length')

    if [ "$COUNT" -eq 0 ]; then
        break
    fi

    ALL_ISSUES=$(echo "$ALL_ISSUES" "$ISSUES" | jq -s '.[0] + .[1]')
    PAGE=$((PAGE + 1))
done

# Filter out unused parameter issues where the parameter name starts with _ (convention for intentionally unused)
ALL_ISSUES=$(echo "$ALL_ISSUES" | jq '[.[] | select((.rule == "kotlin:S1172" and (.message | test("\"_"))) | not)]')

echo "$ALL_ISSUES" | jq '.' > "$ISSUES_FILE"
TOTAL=$(echo "$ALL_ISSUES" | jq 'length')
echo "Exported $TOTAL issues to sonarqube-issues.json"

# Write issues grouped by rule to report file
echo "$ALL_ISSUES" | jq '
  group_by(.rule) | sort_by(-length) | map({
    rule: .[0].rule,
    count: length,
    issues: map({component, severity, type, textRange, message})
  })
' > "$REPORT_FILE"
echo "Exported report to sonarqube-report.json"

echo ""
echo "Code analysis complete, it is available at http://localhost:9000/dashboard?id=trail-sense  (username: admin, password: admin)"
echo ""

if [ "$AUTOSTOP" = true ]; then
    docker stop sonarqube
    docker rm sonarqube
    docker network rm sonarnet
else
    # Pause until the user hits enter, and then remove the sonarqube container
    read -p "Press enter to stop sonarqube"

    docker stop sonarqube
    docker rm sonarqube
    docker network rm sonarnet
fi