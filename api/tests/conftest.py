import os

TEST_TOKEN_SECRET = "test-only-api-secret-not-for-production-0123456789"

os.environ["WORKSHOP_AUTH_TOKEN_SECRET"] = TEST_TOKEN_SECRET
