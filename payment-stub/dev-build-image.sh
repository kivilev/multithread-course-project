#!/bin/bash
set -e

VERSION_TAG="latest"
BASE_IMAGE_NAME="payment-stub"
IMAGE_NAME="${BASE_IMAGE_NAME}:${VERSION_TAG}"

echo "🚀 Building Docker image: $IMAGE_NAME ..."
docker build -t "$IMAGE_NAME" .

echo "✅ Done. Image built:"
docker images "$BASE_IMAGE_NAME"

echo ""
echo "👉 Run container with:"
echo "docker run -it ${IMAGE_NAME}"
