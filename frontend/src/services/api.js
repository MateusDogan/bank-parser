import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({ baseURL: API_URL });

export async function listStatements() {
  const { data } = await api.get('/api/statements');
  return data;
}

export async function uploadStatement(file) {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post('/api/statements/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export function exportStatementCsvUrl(statementId) {
  return `${API_URL}/api/statements/${statementId}/export?format=csv`;
}

export function extractErrorMessage(error) {
  return error.response?.data?.message || error.message || 'Erro desconhecido';
}
