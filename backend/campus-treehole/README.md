# Campus Treehole backend

Requires Java 17 and MySQL 8.4. The default database is `campus_treehole` at `127.0.0.1:3306`.

On the development computer, run the following from this directory in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-local-db.ps1
```

Enter the local MySQL root password at the prompt. The script creates the database if needed and writes `application-local.properties` in this directory. That file is ignored by Git and contains the password in plaintext: do not share it. It is outside `src/main/resources` and is not packaged into the application JAR. This root account is intended for local coursework development only.

The script expects MySQL at `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`; if installed elsewhere, adjust the `mysql` path in the script. It creates the database and local configuration only, so run the SQL files below separately.

To run without that file, set `DB_USER`, `DB_PASSWORD`, and optionally `DB_URL` as environment variables. The application does not create or update database tables automatically.

## Treehole list

Apply `docs/sql/001_create_post.sql` through `docs/sql/006_create_comment.sql` in that order to `campus_treehole` before starting the application. Stop application writes while applying the third script: it reserves existing author and liker IDs before new users are created. Existing post and like rows are retained. Apply 005 only once; it adds a category column to the existing table.

`GET /api/posts` returns treeholes ordered by creation time and ID, newest first. Optional zero-based `page` and `size` parameters default to `0` and `20`; `size` must be between 1 and 100. Invalid values return HTTP 400. The response contains `items`, `page`, `size`, and `total`; each item contains `id`, `content`, `createdAt`, `likeCount`, and `likedByMe`. The last field is calculated for the current user and is false for anonymous requests outside local mode. Author IDs are not returned. An empty database returns an empty `items` array.

Run `CampusTreeholeApplication` in IDEA and visit `http://127.0.0.1:18081/api/posts`. The default HTTP port is 18081 because port 8080 was already in use on the development computer. Set `SERVER_PORT` if a different port is needed.

## Authentication and posting

The mini program can exchange `wx.login()` code using `POST /api/auth/wechat`. Set `WECHAT_APP_ID` and `WECHAT_APP_SECRET` in the backend process environment; never commit or send the secret to the client. These values must belong to the same mini program used by the developer tools. The server issues an expiring bearer token and stores only its hash. A test account or AppID alone may not support the real exchange: verify with the provider. Real WeChat login has not yet been tested with credentials.

With the `local` profile, requests without a token use a fixed development identity and the HTTP server binds to `127.0.0.1`. A request with an invalid token is rejected instead of falling back to the test identity. Outside local mode, publishing and personal actions require a valid bearer token. Use HTTPS and the mini program's configured domain for real remote use. `POST /api/auth/logout` revokes the current bearer session; other sessions remain valid. Sessions also expire after seven days.

For coursework demonstrations, `local` mode also provides account registration and login via `POST /api/auth/local/register` and `POST /api/auth/local/login`. A username must be 3–32 ASCII letters, digits or underscores; a password must be 6–128 characters. Each registered account has its own identity; passwords are stored as salted hashes. These endpoints are disabled outside local mode.

`POST /api/posts` creates a post; `GET /api/posts/mine` lists the current user's posts; `DELETE /api/posts/{id}` deletes only their own post. `POST /api/posts/{id}/likes` and `DELETE /api/posts/{id}/likes` set or remove a like, returning the current `likeCount` and `likedByMe`. Repeating a like operation is safe; a nonexistent post returns HTTP 404. Deleting a post also deletes its likes through the database foreign key.

Posts support the categories `study`, `life`, `mood`, `help`, and `other`. Send `category` when publishing or as an optional list filter; omitted category defaults to `other`. `GET /api/posts/{id}/comments` returns paginated comments newest first; `POST` to the same path publishes a 1–500 character comment for the current user. Deleting a post also removes its comments.
