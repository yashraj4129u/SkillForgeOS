# SkillForge OS

SkillForge OS is a Java Swing application for student project squads and mentoring. Students manage profiles, discover squads, and submit milestone work. Mentors manage squads, assign milestones, and review submissions. Administrators manage users and squads.

## Requirements

- Java 21
- Maven
- MySQL Server

## Database setup

1. Create a MySQL database named `skillforge_db`.
2. Create the application's base tables (`users`, `participants`, `mentors`, `teams`, `milestones`) using the schema supplied with your course/project. The application performs some additive schema setup on connection, but it does not create every base table.
3. Set these environment variables before launching the app:

   - `SKILLFORGE_DB_URL` (optional; defaults to `jdbc:mysql://localhost:3306/skillforge_db`)
   - `SKILLFORGE_DB_USER` (optional; defaults to `root`)
   - `SKILLFORGE_DB_PASSWORD` (required when the MySQL account has a password)

Example in PowerShell for the current terminal only:

```powershell
$env:SKILLFORGE_DB_URL = "jdbc:mysql://localhost:3306/skillforge_db"
$env:SKILLFORGE_DB_USER = "root"
$env:SKILLFORGE_DB_PASSWORD = "your-local-mysql-password"
```

Do not commit real database credentials or production/student data. Each teammate should configure their own local environment variables.

## Build and run

From the project root (the folder containing `pom.xml`):

```powershell
mvn compile
```

Open the project in IntelliJ IDEA and run the application entry point configured for your project. Ensure the MySQL server is running and the database is initialized first.

## Project layout

- `src/main/java/com/skillforge/ui/` - Swing login and role dashboards
- `src/main/java/com/skillforge/db/` - JDBC connection support
- `src/main/resources/db/migration/` - SQL schema migrations
- `src/test/` - project tests

## Team

Add teammate names, contribution areas, and course/instructor details here before submission.
