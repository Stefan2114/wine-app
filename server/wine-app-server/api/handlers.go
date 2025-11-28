package api

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"wine-app-server/model"
	"wine-app-server/store"
	"wine-app-server/websocket"

	"github.com/gorilla/mux"
)

type APIHandler struct {
	store *store.WineStore
	hub   *websocket.Hub
}

func NewAPIHandler(store *store.WineStore, hub *websocket.Hub) *APIHandler {
	return &APIHandler{
		store: store,
		hub:   hub,
	}
}

func (h *APIHandler) RegisterRoutes(r *mux.Router) {
	r.HandleFunc("/wines", h.getWinesHandler).Methods("GET")
	r.HandleFunc("/wines", h.createWineHandler).Methods("POST")
	r.HandleFunc("/wines/{id}", h.updateWineHandler).Methods("PUT")
	r.HandleFunc("/wines/{id}", h.deleteWineHandler).Methods("DELETE")
	r.HandleFunc("/ws", h.serveWsHandler)
}

func (h *APIHandler) serveWsHandler(w http.ResponseWriter, r *http.Request) {
	websocket.ServeWs(h.hub, w, r)
}

func (h *APIHandler) getWinesHandler(w http.ResponseWriter, r *http.Request) {
	wines := h.store.GetWines()
	respondWithJSON(w, http.StatusOK, wines)
}

func (h *APIHandler) createWineHandler(w http.ResponseWriter, r *http.Request) {
	var wine model.Wine
	if err := json.NewDecoder(r.Body).Decode(&wine); err != nil {
		respondWithError(w, http.StatusBadRequest, err.Error())
		return
	}

	if err := h.store.CreateWine(&wine); err != nil {
		respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	msg := websocket.WsMessage{Type: "WINE_ADDED", Payload: wine}
	h.hub.Broadcast <- msg
	log.Printf("Created wine: %s (ID: %d)\n", wine.Name, wine.ID)
	respondWithJSON(w, http.StatusCreated, wine)
}

func (h *APIHandler) updateWineHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])

	var wineUpdate model.Wine
	if err := json.NewDecoder(r.Body).Decode(&wineUpdate); err != nil {
		respondWithError(w, http.StatusBadRequest, err.Error())
		return
	}

	wineUpdate.ID = id

	if err := h.store.UpdateWine(id, wineUpdate); err != nil {
		respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	msg := websocket.WsMessage{Type: "WINE_UPDATED", Payload: wineUpdate}
	h.hub.Broadcast <- msg

	log.Printf("Updated wine (ID: %d)\n", id)
	respondWithJSON(w, http.StatusOK, wineUpdate)
}

func (h *APIHandler) deleteWineHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, _ := strconv.Atoi(vars["id"])

	if err := h.store.DeleteWine(id); err != nil {
		respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	payload := map[string]int{"id": id}
	msg := websocket.WsMessage{Type: "WINE_DELETED", Payload: payload}
	h.hub.Broadcast <- msg
	
	log.Printf("Deleted wine (ID: %d)\n", id)
	w.WriteHeader(http.StatusNoContent)
}

func respondWithError(w http.ResponseWriter, code int, message string) {
	respondWithJSON(w, code, map[string]string{"error": message})
}

func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
