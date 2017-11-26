server {
  listen 80;
  server_name trades.ukora.com;
  root /var/www/trades.ukora.com;
  index index.html index.htm;
  location / {
    auth_basic "Restricted Content";
    auth_basic_user_file /etc/nginx/.htpasswd;
  }
  location /api {
    proxy_pass http://localhost:3000;
    proxy_set_header Host $host;
    auth_basic "Restricted Content";
    auth_basic_user_file /etc/nginx/.htpasswd;
  }
  location /learn {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    auth_basic "Restricted Content";
    auth_basic_user_file /etc/nginx/.htpasswd;
  }
}