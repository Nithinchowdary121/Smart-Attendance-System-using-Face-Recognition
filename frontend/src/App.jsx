import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Students from "./pages/Students";
import Attendance from "./pages/Attendance";
import AttendanceReport from "./pages/AttendanceReport";

function App() {

  return (
    <BrowserRouter>

      <Routes>

        <Route path="/" element={<Login />} />

        <Route path="/dashboard" element={<Dashboard />} />

        <Route path="/students" element={<Students />} />

        <Route path="/attendance" element={<Attendance />} />
        
        <Route path="/attendance-report" element={<AttendanceReport />} />

      </Routes>

    </BrowserRouter>
  )

}

export default App;