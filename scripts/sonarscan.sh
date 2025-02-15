# Run sonarqube

docker run -d --name sonarqube -p 9000:9000 sonarqube:lts

echo "Waiting for sonarqube to start..."

sleep 5

# Wait for the sonarqube container to start (use the api to tell when it is ready)
while [ "$(curl -s http://localhost:9000/api/system/status | jq -r .status)" != "UP" ]; do
    sleep 5
done

echo "Analyzing code..."

docker run --rm \
  -v "$(pwd)/../app/src/main/java:/usr/src" \
  sonarsource/sonar-scanner-cli \
  -Dsonar.projectKey=trail-sense \
  -Dsonar.sources=/usr/src \
  -Dsonar.host.url=http://172.17.0.1:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin \
  -Dsonar.exclusions=**/*.java

echo ""
echo "Code analysis complete, it is available at http://localhost:9000/dashboard?id=trail-sense  (username: admin, password: admin)"
echo ""

# Pause until the user hits enter, and then remove the sonarqube container
read -p "Press enter to stop sonarqube"

docker stop sonarqube
docker rm sonarqube