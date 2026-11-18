# SkillForge OS - Viva Preparation Guide

Use this guide to prepare for the project review. Keep the project open in IntelliJ so you can show the code while explaining it. The search terms below are intended for **Ctrl+Shift+F** (Find in Files); use **Ctrl+F** when the relevant file is already open.

## 1. Project summary

**SkillForge OS** is a Java desktop application for organizing student project squads and mentoring workflows. It uses Java Swing for the interface, MySQL for persistent data, and JDBC to connect Java code to the database.

- **Students** manage profiles, discover and join squads, view milestones, submit work, and track progress.
- **Mentors** manage squads, assign milestones (with links or files), and review student submissions with feedback.
- **Administrators** manage users, squads, and approvals.

**Short presentation:** “SkillForge OS helps students form project squads and complete milestones with mentor guidance. Students submit work, mentors review it, and administrators manage the platform. The desktop UI is built with Swing, and MySQL data is accessed through JDBC.”

## 2. Review 1 - GUI design, database design, JDBC

### GUI design

**Files:** `LoginFrame.java`, `StudentDashboard.java`, `MentorDashboard.java`, `AdminDashboard.java`, `AppTheme.java`.

**Search:** `extends JFrame`, `JTabbedPane`, `JTable`, `JButton`, `styleTabs`.

Explain that the application uses Swing windows, tabs, tables, buttons, forms, and filters. `AppTheme` contains shared styling helpers used to keep dashboard components visually consistent. Demonstrate each role’s screen and describe its workflow.

### Database design

**Files:** `main/resources/db/migration/` and `DBConnection.java`.

**Search:** `CREATE TABLE`, `milestone_submissions`, `activity_events`, `FOREIGN KEY`.

Explain that the database stores users and role-specific data, squads, milestones, submissions, reviews, and activity. A primary key identifies a row; a foreign key relates records across tables. The migration SQL files show schema changes for portfolio fields, collaboration/submissions, and reviews/activity.

### JDBC

**Files:** `DBConnection.java` and the dashboard classes.

**Search:** `DriverManager`, `getConnection`, `PreparedStatement`, `ResultSet`.

Explain that JDBC lets Java connect to MySQL and execute SQL. `DBConnection` provides connections; dashboard code uses prepared statements to run parameterized queries and result sets to read returned rows. Prepared statements help keep user input separate from SQL syntax.

## 3. Review 2 - packages, interfaces, Java libraries, exceptions

### Packages

**Search:** `package com.skillforge`.

The main application code is grouped by responsibility: `com.skillforge.ui` contains screens and UI support, while `com.skillforge.db` contains database connection utilities.

### Interface and polymorphism

**File:** `AppTheme.java`.

**Search:** `implements TableCellRenderer`, `getTableCellRendererComponent`.

`StatusBadgeRenderer` implements Swing’s `TableCellRenderer` interface. It provides the renderer method so status cells can be displayed with custom styling. This is a concrete example of implementing an interface and supplying specialized behavior (polymorphism).

### Java libraries

**Search:** `import javax.swing`, `import java.sql`, `import java.awt`, `java.nio.file`, `java.util`.

Swing/AWT support the desktop UI; `java.sql` provides JDBC types; file APIs support attachment work; and collection utilities store groups of values.

### Exception handling

**Search:** `catch (SQLException`, `throws SQLException`, `try (`.

Database failures are caught and handled in the application. Try-with-resources closes JDBC resources such as connections, statements, and result sets when a block finishes.

### Other Java concepts to recognize

- **Class:** a blueprint, such as `StudentDashboard`.
- **Object:** an instance created from a class.
- **Constructor:** initialization code, such as `StudentDashboard(int userId)`.
- **Method:** behavior defined by a class, such as `loadSquads()`.
- **Encapsulation:** data and behavior live together; fields marked `private` are not directly accessible from other classes.
- **Inheritance:** dashboard windows extend Swing’s `JFrame`.
- **Collections and generics:** `List<Integer>` or `List<String>` stores typed groups of values.

Do not claim method overloading, abstract classes, or other patterns unless you can find and explain a real example in the source.

## 4. Review 3 - software application and presentation

Walk through one complete user journey:

1. A student signs in and views their profile.
2. The student discovers an approved squad and joins it.
3. A mentor assigns a milestone to one or more squads and can provide a link or file.
4. The student submits a link or file for the milestone.
5. The mentor reviews the submission and provides a status and feedback.
6. The dashboards show milestone status and squad progress; administrators oversee users and squads.

The exact screens and available actions depend on the current database state. Show the workflow in the running app if the database is available.

## 5. Multithreading - the project example

**File:** `StudentDashboard.java`, method `loadSquads()`.

**Search:** `SwingWorker`, `doInBackground`, `done()`, `cancel(true)`.

The squad-list query runs inside `doInBackground()`, away from Swing’s event-dispatch thread. This helps the interface remain responsive while the database query runs. `done()` runs on Swing’s UI thread and applies the results to the table. A newer refresh cancels the previous unfinished worker to avoid applying stale results.

**Presentation wording:** “I used `SwingWorker` to load squads in the background. The JDBC query runs in `doInBackground()`, so it doesn’t block the Swing UI thread. When it finishes, `done()` updates the table safely on the UI thread.”

`SwingUtilities.invokeLater` schedules work on Swing’s UI thread; it is not by itself a background worker. `synchronized` in `DBConnection` coordinates schema setup and is also not the squad-loading worker.

## 6. Transactions and safe database updates

**Search:** `setAutoCommit(false)`, `commit()`, `rollback()`, `FOR UPDATE`.

Some multi-step database operations use transactions: auto-commit is disabled, related statements are run, and changes are committed together. If a step fails, rollback can undo the partial transaction. A `FOR UPDATE` query can lock selected rows during a transaction to prevent conflicting changes.

## 7. Likely viva questions

**What problem does the project solve?**

It organizes student project squads and milestone work, connecting student submissions with mentor review and administrative oversight.

**Why use JDBC and prepared statements?**

JDBC connects Java to the relational database. Prepared statements execute parameterized SQL and keep values separate from the query text.

**Why use SwingWorker?**

Database work can take time. SwingWorker performs that work in the background and returns results for safe UI updates when it completes.

**What is polymorphism in your code?**

The status badge renderer implements Swing’s `TableCellRenderer` interface and supplies the component-rendering behavior used for table cells.

**What is a transaction?**

A group of database changes treated as one unit: commit them together on success, or roll them back if the operation fails.

**How do you handle database errors?**

Database operations catch `SQLException` and report or handle the failure. Try-with-resources closes database resources reliably.

**What would you improve next?**

Possible future work includes adding automated tests for core workflows, improving accessibility and input validation, and adding more dashboard feedback for loading and database errors. Present these as future improvements, not completed features.

## 8. Last-minute Ctrl+Shift+F checklist

`SwingWorker` · `doInBackground` · `done()` · `implements TableCellRenderer` · `getTableCellRendererComponent` · `DriverManager` · `PreparedStatement` · `ResultSet` · `catch (SQLException` · `setAutoCommit(false)` · `commit()` · `rollback()` · `CREATE TABLE` · `package com.skillforge`

## 9. Viva tips

- Explain the feature in your own words, then point to its class and method.
- Trace one complete workflow instead of listing screens without context.
- Be clear about the difference between background work (`SwingWorker`), Swing UI-thread scheduling (`invokeLater`), and synchronization (`synchronized`).
- If something is not implemented, describe it as a future improvement rather than claiming it is already present.
- Avoid showing database passwords or other credentials during a screen share.
