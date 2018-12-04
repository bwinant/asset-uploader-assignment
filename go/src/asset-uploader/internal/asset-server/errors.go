package asset_server

import (
	"github.com/aws/aws-sdk-go/aws/awserr"
)

type RestError interface {
	// Extend generic error interface
	error

	// HTTP status code to return in response
	Status() int

	// Message to return in response
	Message() string

	// Original error, if any
	Nested() error
}

type baseError struct {
	message string
	status  int
	nested  error
}

func (e baseError) Error() string {
	return e.Message()
}

func (e baseError) Status() int {
	return e.status
}

func (e baseError) Message() string {
	return e.message
}

func (e baseError) Nested() error {
	return e.nested
}

func ServerError(message string, origError error) RestError {
	return &baseError{status: 500, message: message, nested: origError}
}

func NotFoundError(message string) RestError {
	return &baseError{status: 404, message: message}
}

func isNotFound(err error) bool {
	if awsErr, ok := err.(awserr.Error); ok {
		return awsErr.Code() == "NotFound"
	}
	return false
}
