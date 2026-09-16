import { Routes, Route, NavLink } from 'react-router-dom';
import Upload from './pages/Upload.jsx';
import Statements from './pages/Statements.jsx';

export default function App() {
  return (
    <div className="app">
      <header>
        <h1>Bank Parser</h1>
        <nav>
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
            Upload
          </NavLink>
          <NavLink to="/statements" className={({ isActive }) => (isActive ? 'active' : '')}>
            Meus Extratos
          </NavLink>
        </nav>
      </header>

      <Routes>
        <Route path="/" element={<Upload />} />
        <Route path="/statements" element={<Statements />} />
      </Routes>
    </div>
  );
}
