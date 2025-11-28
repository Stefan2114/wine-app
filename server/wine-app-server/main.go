package main

import (
	"database/sql"
	"log"
	"net/http"
	"wine-app-server/api"
	"wine-app-server/store"
	"wine-app-server/websocket"

	"github.com/gorilla/mux"
	_ "github.com/jackc/pgx/v5/stdlib"
)

func main() {
	connStr := "postgres://stef:1234@localhost:5432/wines_db?sslmode=disable"
	db, err := sql.Open("pgx", connStr)
	if err != nil {
		log.Fatalf("Unable to connect to database: %v\n", err)
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Fatalf("Database ping failed: %v\n", err)
	}
	log.Println("Successfully connected to PostgreSQL!")

	s := store.New(db)
	hub := websocket.NewHub()
	go hub.Run()
	h := api.NewAPIHandler(s, hub)

	r := mux.NewRouter()
	h.RegisterRoutes(r)

	port := ":8080"
	log.Printf("Starting server on %s\n", port)
	if err := http.ListenAndServe(port, r); err != nil {
		log.Fatal(err)
	}
}
