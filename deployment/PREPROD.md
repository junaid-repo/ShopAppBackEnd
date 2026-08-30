# ClearBills backend preprod setup

The repository deploys only two branches:

- `main` uses the GitHub `production` environment and the existing `shop-api` and `user-api` services.
- `preprod` uses the GitHub `preprod` environment and the isolated `shop-api-preprod` and `user-api-preprod` services.

All other branches can build in pull-request checks, but cannot deploy through these workflows.

## 1. GitHub configuration

Create GitHub environments named `production` and `preprod`. Configure these three secrets in each environment:

- `ORACLE_HOST`
- `ORACLE_USERNAME`
- `ORACLE_SSH_KEY`

When both environments use the same Oracle instance, the values can currently be identical. When preprod moves to its own instance, only the secrets in the `preprod` environment need to change.

Add a required reviewer to the `production` environment so a production deployment requires approval.

## 2. Database and Redis

Create a dedicated MySQL database and user for preprod. Both services currently share the same application data model, so their environment files point to the same isolated `clearbills_preprod` database.

Run a separate Redis instance on port `6380`, or change `REDIS_HOST` and `REDIS_PORT` in both environment files to another isolated Redis service. Do not reuse the production Redis instance.

## 3. Environment files

Copy the examples without committing their real values:

```bash
sudo install -d -m 0750 /etc/clearbills/preprod
sudo install -m 0640 deployment/env/shop-preprod.env.example /etc/clearbills/preprod/shop.env
sudo install -m 0640 deployment/env/user-preprod.env.example /etc/clearbills/preprod/user.env
```

Replace every `replace-me` value. The two services must use the same preprod `JWT_SECRET` and `INTERNAL_API_KEY`, but these values must differ from production.

The preprod scheduler cron values are `-`, which disables customer-facing and automated scheduled work until it is intentionally enabled.

## 4. Install the services

```bash
sudo install -d -o ubuntu -g ubuntu /home/ubuntu/clearbills-preprod
sudo install -d -o ubuntu -g ubuntu /home/ubuntu/clearbills-preprod/uploads/profiles
sudo install -d -o ubuntu -g ubuntu /home/ubuntu/clearbills-preprod/uploads/logos
sudo install -d -o ubuntu -g ubuntu /home/ubuntu/clearbills-preprod/uploads/signs
sudo install -m 0644 deployment/systemd/shop-api-preprod.service /etc/systemd/system/shop-api-preprod.service
sudo install -m 0644 deployment/systemd/user-api-preprod.service /etc/systemd/system/user-api-preprod.service
sudo systemctl daemon-reload
sudo systemctl enable shop-api-preprod user-api-preprod
```

The services can be started after their first JAR files are deployed by GitHub Actions.

## 5. Nginx and DNS

Point these DNS records to the preprod Oracle instance:

- `shopapi-preprod.clearbills.info`
- `userapi-preprod.clearbills.info`

Install `deployment/nginx/clearbills-preprod.conf`, validate Nginx, reload it, and issue TLS certificates for both hostnames. Only ports 80 and 443 should be publicly available; ports 8180, 8181, 7182, 7183, MySQL, and Redis should remain private.

## 6. First deployment

After the server preparation is complete, push or merge a backend change into `preprod`. Both workflows deploy to `/home/ubuntu/clearbills-preprod`, restart only the preprod services, and verify their private actuator health endpoints.

Merging `preprod` into `main` deploys production. No environment property files or databases are copied during promotion; only application code is promoted.
