import { useRef, useState } from 'react';
import { uploadStatement, extractErrorMessage } from '../services/api.js';

export default function Upload() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState(null); // { type: 'success'|'error'|'info', message }
  const [isDragging, setIsDragging] = useState(false);

  const fileInputRef = useRef(null);

  function handleFileSelect(selected) {
    if (!selected) return;
    if (selected.type !== 'application/pdf') {
      setStatus({ type: 'error', message: 'Apenas arquivos PDF são aceitos.' });
      return;
    }
    setFile(selected);
    setStatus(null);
  }

  function handleDrop(e) {
    e.preventDefault();
    setIsDragging(false);
    handleFileSelect(e.dataTransfer.files[0]);
  }

  async function handleStart() {
    if (!file) return;
    setStatus({ type: 'info', message: 'Enviando...' });
    try {
      await uploadStatement(file);
      setStatus({ type: 'success', message: 'Extrato enviado e processado com sucesso!' });
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      setStatus({ type: 'error', message: extractErrorMessage(err) });
    }
  }

  return (
    <div className="upload-page">
      <div
        className={`dropzone ${isDragging ? 'dragging' : ''} ${file ? 'has-file' : ''}`}
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          hidden
          onChange={(e) => handleFileSelect(e.target.files[0])}
        />

        <svg className="dropzone-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M6 2h9l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinejoin="round"
          />
          <path d="M15 2v5h5" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
          <path d="M8 13h8M8 17h5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        </svg>

        <svg
          className="dropzone-icon dropzone-icon-cloud"
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M7 18a4 4 0 0 1-.5-7.97A5 5 0 0 1 16.5 8.5 4 4 0 0 1 16 18H7Z"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinejoin="round"
          />
          <path
            d="M12 12v6M9.5 15.5 12 13l2.5 2.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        {file ? (
          <>
            <p className="dropzone-title">{file.name}</p>
            <p className="dropzone-subtitle">clique para trocar o arquivo</p>
          </>
        ) : (
          <>
            <p className="dropzone-title">Arraste o PDF aqui</p>
            <p className="dropzone-subtitle">ou clique para escolher</p>
          </>
        )}
      </div>

      <button type="button" className="pill-button" disabled={!file} onClick={handleStart}>
        Iniciar
      </button>

      {status && <div className={`feedback ${status.type}`}>{status.message}</div>}
    </div>
  );
}
