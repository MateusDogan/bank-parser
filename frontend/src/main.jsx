import React from 'react';
import ReactDOM from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import App from './App.jsx';
import './index.css';

// HashRouter (#/statements) em vez de BrowserRouter: GitHub Pages nao tem
// como redirecionar toda rota pra index.html, entao um refresh em /statements
// daria 404. Com hash, o roteamento e so client-side.
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <HashRouter>
      <App />
    </HashRouter>
  </React.StrictMode>
);
