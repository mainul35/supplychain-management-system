mvn clean install -DskipTests -Dspring.profiles.active=dev && docker image build -t bs-userinfo:1.0 .