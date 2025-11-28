package model

type Wine struct {
	ID             int     `json:"id"`
	Name           string  `json:"name"`
	Price          float64 `json:"price"`
	ProductionDate string  `json:"productionDate"`
	Origin         string  `json:"origin,omitempty"`
	AlcoholDegree  float64 `json:"alcoholDegree"`
}
