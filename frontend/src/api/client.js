import axios from 'axios';
import { places as fallbackPlaces } from '../data.js';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/',
  timeout: 5000
});

export async function fetchTourismPlaces() {
  try {
    const { data } = await apiClient.get('/mocks/tourism-places.json');
    return Array.isArray(data?.data) ? data.data : fallbackPlaces;
  } catch {
    return fallbackPlaces;
  }
}
