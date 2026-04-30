# Docker usage

## Local

로컬에서는 Redis만 Docker로 실행하고, 스프링 애플리케이션은 IDE 또는 로컬 JVM에서 실행합니다.

```bash
docker compose -f compose.local.yml up -d
```

스프링 실행 시 로컬 프로파일을 사용합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Production on EC2

1. `.env.prod.example` 파일을 복사해서 `.env.prod`를 만듭니다.
2. 비밀번호와 포트를 실제 값으로 수정합니다.
3. 아래 명령으로 애플리케이션, PostgreSQL, Redis, Prometheus, Grafana를 함께 실행합니다.

```bash
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

중지:

```bash
docker compose --env-file .env.prod -f compose.prod.yml down
```

Grafana 기본 포트는 `3000`(prod), `3001`(dev)이며 `.env.*`에서 변경할 수 있습니다.
