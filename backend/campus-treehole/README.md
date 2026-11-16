# Campus Treehole backend

Requires Java 17 and MySQL 8.4. The default database is `campus_treehole` at `127.0.0.1:3306`.

On the development computer, run the following from this directory in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-local-db.ps1
```

Enter the local MySQL root password at the prompt. The script creates the database if needed and writes `application-local.properties` in this directory. That file is ignored by Git and contains the password in plaintext: do not share it. It is outside `src/main/resources` and is not packaged into the application JAR. This root account is intended for local coursework development only.

To run without that file, set `DB_USER`, `DB_PASSWORD`, and optionally `DB_URL` as environment variables. The application does not create or update database tables automatically.

## Treehole list

Apply `docs/sql/001_create_post.sql` in the project root to the `campus_treehole` database before starting the application. The author ID is stored for future login integration; no user table or author foreign key is created yet.

`GET /api/posts` returns treeholes ordered by creation time and ID, newest first. Optional zero-based `page` and `size` parameters default to `0` and `20`; `size` must be between 1 and 100. Invalid values return HTTP 400. The response contains `items`, `page`, `size`, and `total`; each item contains `id`, `content`, and `createdAt`. Author IDs are not returned. An empty database returns an empty `items` array.

Run `CampusTreeholeApplication` in IDEA and visit `http://127.0.0.1:18081/api/posts`. The default HTTP port is 18081 because port 8080 was already in use on the development computer. Set `SERVER_PORT` if a different port is needed.

## Publishing locally

With the `local` Spring profile enabled, `POST /api/posts` accepts JSON such as `{"content":"Hello treehole"}` and responds with HTTP 201 and the new post. Content is trimmed and must contain 1–1000 characters. This temporary endpoint uses a fixed development identity and the `local` profile binds the HTTP server to `127.0.0.1`. It is for local development only; real WeChat login has not yet been implemented. Outside the `local` profile the publishing endpoint is disabled.

The same local development identity can list its posts with `GET /api/posts/mine?page=0&size=20`. `DELETE /api/posts/{id}` removes only a post belonging to this identity and responds with HTTP 204. A missing post or a post belonging to another user returns HTTP 404. Both endpoints are disabled outside the `local` profile. All development sessions share the fixed test identity; this is not real user authentication.
