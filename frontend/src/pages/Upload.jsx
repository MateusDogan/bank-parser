import { useEffect, useState } from 'react';
import { listClients, createClient, uploadStatement, extractErrorMessage } from '../services/api.js';

export default function Upload() {
  const [clients, setClients] = useState([]);
  const [clientId, setClientId] = useState('');
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState(null); // { type: 'success'|'error'|'info', message }

  const [newClientName, setNewClientName] = useState('');
  const [newClientDocument, setNewClientDocument] = useState('');
  const [creatingClient, setCreatingClient] = useState(false);

  useEffect(() => {
    refreshClients();
  }, []);

  async function refreshClients() {
    try {
      const data = await listClients();
      setClients(data);
      if (data.length > 0 && !clientId) {
        setClientId(data[0].id);
      }
    } catch (err) {
      setStatus({ type: 'error', message: extractErrorMessage(err) });
    }
  }

  async function handleCreateClient(e) {
    e.preventDefault();
    setCreatingClient(true);
    setStatus(null);
    try {
      const client = await createClient(newClientName, newClientDocument);
      setNewClientName('');
      setNewClientDocument('');
      await refreshClients();
      setClientId(client.id);
      setStatus({ type: 'success', message: `Cliente "${client.name}" cadastrado.` });
    } catch (err) {
      setStatus({ type: 'error', message: extractErrorMessage(err) });
    } finally {
      setCreatingClient(false);
    }
  }

  async function handleUpload(e) {
    e.preventDefault();
    if (!file || !clientId) return;

    setStatus({ type: 'info', message: 'Enviando...' });
    try {
      await uploadStatement(file, clientId);
      setStatus({ type: 'success', message: 'Extrato enviado e processado com sucesso!' });
      setFile(null);
      e.target.reset();
    } catch (err) {
      setStatus({ type: 'error', message: extractErrorMessage(err) });
    }
  }

  return (
    <div>
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1rem' }}>Novo Cliente</h2>
        <form onSubmit={handleCreateClient}>
          <label htmlFor="newClientName">Nome</label>
          <input
            id="newClientName"
            type="text"
            value={newClientName}
            onChange={(e) => setNewClientName(e.target.value)}
            required
          />

          <label htmlFor="newClientDocument">CNPJ / CPF</label>
          <input
            id="newClientDocument"
            type="text"
            value={newClientDocument}
            onChange={(e) => setNewClientDocument(e.target.value)}
            placeholder="12.345.678/0001-99"
            required
          />

          <button type="submit" disabled={creatingClient}>
            {creatingClient ? 'Cadastrando...' : 'Cadastrar Cliente'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0, fontSize: '1rem' }}>Enviar Extrato (PDF)</h2>
        <form onSubmit={handleUpload}>
          <label htmlFor="clientSelect">Cliente</label>
          <select
            id="clientSelect"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            required
          >
            <option value="" disabled>
              {clients.length === 0 ? 'Nenhum cliente cadastrado' : 'Escolha um cliente'}
            </option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.document})
              </option>
            ))}
          </select>

          <label htmlFor="pdfFile">Arquivo PDF</label>
          <input
            id="pdfFile"
            type="file"
            accept="application/pdf"
            onChange={(e) => setFile(e.target.files[0])}
            required
          />

          <button type="submit" disabled={!file || !clientId}>
            Enviar
          </button>
        </form>

        {status && <div className={`feedback ${status.type}`}>{status.message}</div>}
      </div>
    </div>
  );
}
