package asset_server

import (
	"encoding/json"
	"log"
	"net/http"
)

type errorResponse struct {
	Message string `json:"error"`
}

func sendSuccess(w http.ResponseWriter, v interface{}) {
	send(w, 200, v)
}

func sendError(w http.ResponseWriter, sc int, message string) {
	res := &errorResponse{Message: message}
	send(w, sc, res)
}

func send(w http.ResponseWriter, sc int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(sc)

	if v != nil {
		if err := json.NewEncoder(w).Encode(v); err != nil {
			log.Println(err)
		}
	}
}
