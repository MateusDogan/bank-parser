import { useEffect, useState } from 'react';
import { listStatements, exportStatementCsvUrl, extractErrorMessage } from '../services/api.js';

export default function Statements() {
  const [statements, setStatements] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    listStatements()
      .then(setStatements)
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);

  if (error) {
    return <div className="feedback error">{error}</div>;
  }

  return (
    <div className="card">
      <h2 style={{ marginTop: 0, fontSize: '1rem' }}>Meus Extratos</h2>

      {statements.length === 0 ? (
        <p className="empty">Nenhum extrato enviado ainda.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Cliente</th>
              <th>Arquivo</th>
              <th>Transações</th>
              <th>Enviado em</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {statements.map((s) => (
              <tr key={s.id}>
                <td>{s.clientName}</td>
                <td>{s.originalFilename}</td>
                <td>{s.transactionCount}</td>
                <td>{new Date(s.uploadedAt).toLocaleString('pt-BR')}</td>
                <td>
                  <a href={exportStatementCsvUrl(s.id)}>
                    <button type="button">Baixar CSV</button>
                  </a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
