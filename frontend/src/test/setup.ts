import '@testing-library/jest-dom/vitest'

// Stabilize date formatting across local dev (Windows) and CI (Linux).
process.env.TZ = 'UTC'
