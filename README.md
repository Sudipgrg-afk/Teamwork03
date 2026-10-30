# Teamwork03 — Java Shop App

A small MVC-style e-commerce demo built in Java, backed by SQLite. Browse products by category, view product details, and add items to a cart.

## Structure

```
src/Team/
├── controller/   # MainController — request routing
├── entity/       # Product, CartItem — domain models
├── model/        # ProductModel, CartService — data access & cart logic
├── view/         # ProductScreen — rendering
└── web/          # CartServer — servlet entry point
```

- `Teamwork03.db` — SQLite database with sample product data
- `index.html`, `category-*.html`, `Details/*.html` — generated storefront pages
- `style.css` — storefront styling

## Running

This is an Eclipse-managed Java EE (servlet) project.

1. Import into Eclipse as an existing project.
2. Add the SQLite JDBC driver (`sqlite-jdbc`) to the build path — not included in this repo.
3. Deploy to a servlet container (e.g. Tomcat) and run.

## Features

- Category-based product browsing
- Product detail pages
- Cart add/update via `CartService`
- SQLite-backed product catalog
