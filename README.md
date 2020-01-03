## Instorage Vaka Device Manager

```
Dev:
logback-spring.xml -> set property for dev
application.properties -> set logging-file-path for dev

Prod:
logback-spring.xml -> set property for prod
application.properties -> set logging-file-path for prod
```

```
Docker run:
1. mvn clean install
2. mvn dockerfile:build
3. docker-compose up
```