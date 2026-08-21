FROM axllent/mailpit:v1.30

COPY scripts/docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod 755 /docker-entrypoint.sh

ENV MP_MAX_MESSAGES="1000"
ENV MP_SMTP_AUTH_ACCEPT_ANY="true"
ENV MP_SMTP_AUTH_ALLOW_INSECURE="true"
ENV MP_SMTP_BIND_ADDR="0.0.0.0:1025"
ENV MP_UI_BIND_ADDR="0.0.0.0:8025"

ENTRYPOINT ["/docker-entrypoint.sh"]