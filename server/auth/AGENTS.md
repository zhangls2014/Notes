# Notes authentication service

- Put application TypeScript in `src/`, automated checks in `test/`, and schema definitions in `sql/`.
- Maintain the client contract in `openapi.yaml` and keep domain association examples in `association/`.
- Use Node.js 24 and the npm lockfile. Run `npm run check` and `npm test` before handing off.
- Keep credentials in process environment variables. Treat database initialization, HTTPS hosting changes, and deployment as separately authorized operations.
