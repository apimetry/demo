package main

import (
	"context"
	"github.com/gin-gonic/gin"
	"go.opentelemetry.io/contrib/instrumentation/github.com/gin-gonic/gin/otelgin"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	tracesdk "go.opentelemetry.io/otel/sdk/trace"
	"os"
)

func main() {
	router := gin.Default()

	router.Use(otelgin.Middleware("app"))

	router.GET("/ping", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "pong",
		})
	})
	router.Run()
}

func InitTracer() func(context.Context) error {
	exporter, err := otlptrace.New(
		context.Background(),
		otlptracegrpc.NewClient(otlptracegrpc.WithEndpoint(os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))),
	)

	if err != nil {
		panic(err)
	}

	resources, err := resource.New(
		context.Background(),
		resource.WithAttributes(
			attribute.String("library.language", "go"),
		),
	)
	if err != nil {
		//log.Print("Could not set resources: ", err)
	}

	otel.SetTracerProvider(
		tracesdk.NewTracerProvider(
			tracesdk.WithSampler(tracesdk.AlwaysSample()),
			tracesdk.WithBatcher(exporter),
			tracesdk.WithResource(resources),
		),
	)
	// Baggage may submit too much sensitive data for production
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(propagation.TraceContext{}, propagation.Baggage{}))

	return exporter.Shutdown
}
