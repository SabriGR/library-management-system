# 📚 Library Management System - REST API

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)
![Hibernate](https://img.shields.io/badge/Hibernate-ORM-blue.svg)
![Maven](https://img.shields.io/badge/Maven-Build-red.svg)

A robust and scalable RESTful API built to manage core library operations. This project was developed to demonstrate backend engineering skills, focusing on **clean code principles**, **layered architecture**, and **best practices** in Java Spring Boot.

## 🚀 Tech Stack
* **Core:** Java 21, Spring Boot
* **Data Access:** Spring Data JPA, Hibernate ORM
* **Database:** MySQL / H2 (In-memory for easy testing)
* **Utilities:** Lombok (for boilerplate reduction), Maven

## 🏗️ System Architecture
The application strictly follows a standard **Layered Architecture** to ensure separation of concerns and maintainability:
* `Controller Layer`: Exposes REST endpoints and handles HTTP requests/responses.
* `Service Layer`: Contains the core business logic (e.g., availability checks, due date calculations).
* `Repository Layer`: Interfaces directly with the database using Spring Data JPA.
* `Entity Layer`: Defines the database schema and relationships (`User`, `Book`, `BorrowingRecord`).

## ✨ Core Features
* **Automated Data Seeding:** System automatically initializes with test data (Users, Books, and historical borrowing records) on startup for immediate testing.
* **Borrowing Logic:** Validates book availability before assigning it to a user and automatically sets a 14-day due date.
* **Inventory Management:** Automatically increments or decrements the available copies of a book based on borrow/return actions.
* **Overdue Tracking:** Identifies and lists all active borrowings that have passed their designated return date.

## 🔌 API Endpoints

| HTTP Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/library/borrow?userId={id}&bookId={id}` | Creates a new borrowing record if the book is available. |
| `POST` | `/api/library/return/{recordId}` | Marks a book as returned and updates inventory. |
| `GET` | `/api/library/user/{userId}/active` | Retrieves all currently borrowed books for a specific user. |
| `GET` | `/api/library/overdue` | Fetches a list of all overdue borrowing records. |

## 🛠️ How to Run Locally

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/SabriGR/library-management-system.git](https://github.com/SabriGR/library-management-system.git)
Navigate to the directory:

Bash
cd library-management-system
Database Configuration:
The project is configured to run easily. Ensure your MySQL credentials match the src/main/resources/application.properties file, or switch to H2 for instant testing.

Run the application:

Bash
./mvnw spring-boot:run
The API will be accessible at http://localhost:8080.
