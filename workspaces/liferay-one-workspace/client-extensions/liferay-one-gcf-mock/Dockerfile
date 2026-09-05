FROM liferay/caddy:latest

COPY mock/Caddyfile /etc/gcf-mock/Caddyfile
COPY mock/generate-fixtures.sh /tmp/generate-fixtures.sh

RUN /tmp/generate-fixtures.sh /etc/gcf-mock && rm /tmp/generate-fixtures.sh

ENTRYPOINT ["caddy"]

CMD ["run", "--adapter", "caddyfile", "--config", "/etc/gcf-mock/Caddyfile"]