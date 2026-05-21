# Composables

This directory contains Vue composables (composable functions) for reusable logic.

## Usage

Composables should be named using the `use` prefix, e.g., `useKnowledge.js`.

Example:
```javascript
import { useKnowledge } from '@/composables/useKnowledge'

const { fetchBases, createBase } = useKnowledge()
```
