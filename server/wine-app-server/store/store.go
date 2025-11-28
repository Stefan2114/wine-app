package store

import (
	"database/sql"
	"fmt"
	"log"
	"wine-app-server/model"
)

type WineStore struct {
	db *sql.DB
}

func New(db *sql.DB) *WineStore {
	return &WineStore{
		db: db,
	}
}

func (s *WineStore) GetWines() []model.Wine {
	rows, err := s.db.Query("SELECT id, name, price, productionDate, origin, alcoholDegree FROM wines")
	if err != nil {
		log.Printf("Error querying wines: %v", err)
		return []model.Wine{}
	}
	defer rows.Close()

	var wines []model.Wine
	for rows.Next() {
		var wine model.Wine
		if err := rows.Scan(
			&wine.ID,
			&wine.Name,
			&wine.Price,
			&wine.ProductionDate,
			&wine.Origin,
			&wine.AlcoholDegree,
		); err != nil {
			log.Printf("Error scanning row: %v", err)
			continue
		}
		wines = append(wines, wine)
	}
	return wines
}

func (s *WineStore) CreateWine(wine *model.Wine) error {
	query := `
		INSERT INTO wines (name, price, productionDate, origin, alcoholDegree)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id
	`
	err := s.db.QueryRow(
		query,
		wine.Name,
		wine.Price,
		wine.ProductionDate,
		wine.Origin,
		wine.AlcoholDegree,
	).Scan(&wine.ID) // Scan the new ID back into the wine object

	return err
}

func (s *WineStore) UpdateWine(id int, wine model.Wine) error {
	query := `
		UPDATE wines
		SET name = $1, price = $2, productionDate = $3, origin = $4, alcoholDegree = $5
		WHERE id = $6
	`
	result, err := s.db.Exec(
		query,
		wine.Name,
		wine.Price,
		wine.ProductionDate,
		wine.Origin,
		wine.AlcoholDegree,
		id,
	)

	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rowsAffected == 0 {
		return fmt.Errorf("wine with ID %d not found", id)
	}

	return nil
}

func (s *WineStore) DeleteWine(id int) error {
	result, err := s.db.Exec("DELETE FROM wines WHERE id = $1", id)
	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rowsAffected == 0 {
		return fmt.Errorf("wine with ID %d not found", id)
	}

	return nil
}
