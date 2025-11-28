package websocket

import (
	"encoding/json"
	"log"
)

// WsMessage defines the shape of a broadcast message
type WsMessage struct {
	Type    string      `json:"type"`
	Payload interface{} `json:"payload"`
}

type Hub struct {
	clients    map[*Client]bool
	Broadcast  chan WsMessage
	register   chan *Client
	unregister chan *Client
}

func NewHub() *Hub {
	return &Hub{Broadcast: make(chan WsMessage),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		clients:    make(map[*Client]bool)}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.clients[client] = true
			log.Println("Client connected:", client.conn.RemoteAddr().String())
		case client := <-h.unregister:
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				close(client.send)
				log.Println("Client disconnected:", client.conn.RemoteAddr().String())
			}
		case message := <-h.Broadcast:
			// Convert message to JSON
			msgBytes, err := json.Marshal(message)
			if err != nil {
				log.Printf("Error marshaling broadcast message: %v", err)
				continue
			}

			// Send to all clients
			for client := range h.clients {
				select {
				case client.send <- msgBytes:
				default:
					close(client.send)
					delete(h.clients, client)
				}
			}
		}
	}
}
