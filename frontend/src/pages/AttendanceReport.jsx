import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import API from "../services/api";
import { ClipboardCheck, Calendar, Search, Download, Filter, User, ChevronLeft, Book } from "lucide-react";
import jsPDF from "jspdf";
import "jspdf-autotable";

const AttendanceReport = () => {
  const [attendance, setAttendance] = useState([]);
  const [students, setStudents] = useState({});
  const [subjects, setSubjects] = useState({});
  const [loading, setLoading] = useState(false);
  const [dateFilter, setDateFilter] = useState(new Date().toISOString().split('T')[0]);
  const [searchTerm, setSearchTerm] = useState("");
  const navigate = useNavigate();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [attRes, stuRes, subRes] = await Promise.all([
        API.get(`/attendance/report?date=${dateFilter}`),
        API.get("/students"),
        API.get("/subjects")
      ]);
      
      const stuMap = {};
      stuRes.data.forEach(s => stuMap[s.id] = s);
      setStudents(stuMap);

      const subMap = {};
      subRes.data.forEach(s => subMap[s.id] = s);
      setSubjects(subMap);

      setAttendance(attRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || user.role !== "ADMIN") {
      navigate("/");
      return;
    }
    fetchData();
  }, [dateFilter]);

  const exportPDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(20);
    doc.text("Attendance Report", 14, 22);
    doc.setFontSize(11);
    doc.setTextColor(100);
    doc.text(`Date: ${dateFilter}`, 14, 30);
    
    const tableData = attendance.map(record => {
      const student = students[record.studentId] || { name: "Unknown", rollNumber: "N/A" };
      const subject = subjects[record.subjectId] || { name: "N/A" };
      return [student.name, student.rollNumber, subject.name, record.time, record.status];
    });

    doc.autoTable({
      head: [['Student Name', 'Roll Number', 'Subject', 'Time', 'Status']],
      body: tableData,
      startY: 40,
      theme: 'grid',
      headStyles: { fillStyle: '#6366f1', textColor: 255 },
      styles: { fontSize: 10 }
    });

    doc.save(`Attendance_Report_${dateFilter}.pdf`);
  };

  const filteredAttendance = attendance.filter(record => {
    const student = students[record.studentId];
    const subject = subjects[record.subjectId];
    if (!student) return false;
    const searchStr = searchTerm.toLowerCase();
    return student.name.toLowerCase().includes(searchStr) || 
           student.rollNumber.toLowerCase().includes(searchStr) ||
           (subject && subject.name.toLowerCase().includes(searchStr));
  });

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-6xl mx-auto">
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
          <div>
            <Link to="/dashboard" className="flex items-center gap-2 text-indigo-600 font-bold text-sm mb-4 hover:gap-3 transition-all">
              <ChevronLeft className="w-4 h-4" /> Back to Dashboard
            </Link>
            <h1 className="text-4xl font-black text-slate-800">Attendance Intelligence</h1>
            <p className="text-slate-400 mt-1 font-medium">Daily participation metrics and biometric logs</p>
          </div>
          <div className="flex gap-3">
            <button 
              onClick={exportPDF}
              disabled={attendance.length === 0}
              className="bg-slate-900 hover:bg-indigo-600 text-white px-8 py-4 rounded-2xl font-black flex items-center gap-3 shadow-xl shadow-slate-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Download className="w-5 h-5" /> Export Intelligence
            </button>
          </div>
        </header>

        {/* Filters */}
        <div className="bg-white p-8 rounded-[2.5rem] shadow-sm border border-slate-100 mb-8 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="space-y-3">
            <label className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] flex items-center gap-2">
              <Calendar className="w-4 h-4 text-indigo-500" /> Target Date
            </label>
            <input 
              type="date"
              className="w-full bg-slate-50 border-none rounded-2xl px-5 py-4 font-bold text-slate-700 outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
            />
          </div>
          <div className="space-y-3">
            <label className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] flex items-center gap-2">
              <Filter className="w-4 h-4 text-emerald-500" /> Verification Status
            </label>
            <select className="w-full bg-slate-50 border-none rounded-2xl px-5 py-4 font-bold text-slate-700 outline-none focus:ring-2 focus:ring-indigo-500 transition-all appearance-none">
              <option>All Records</option>
              <option>Biometric Verified</option>
              <option>Manual Override</option>
            </select>
          </div>
          <div className="space-y-3">
            <label className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] flex items-center gap-2">
              <Search className="w-4 h-4 text-amber-500" /> Dynamic Search
            </label>
            <input 
              type="text"
              placeholder="Search student, ID, or subject..."
              className="w-full bg-slate-50 border-none rounded-2xl px-5 py-4 font-bold text-slate-700 outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Report Table */}
        <div className="bg-white rounded-[2.5rem] shadow-sm border border-slate-100 overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50 border-b border-slate-100">
              <tr>
                <th className="px-10 py-6 text-[10px] font-black text-slate-400 uppercase tracking-widest">Student Profile</th>
                <th className="px-10 py-6 text-[10px] font-black text-slate-400 uppercase tracking-widest">Subject</th>
                <th className="px-10 py-6 text-[10px] font-black text-slate-400 uppercase tracking-widest">Identification</th>
                <th className="px-10 py-6 text-[10px] font-black text-slate-400 uppercase tracking-widest">Timestamp</th>
                <th className="px-10 py-6 text-[10px] font-black text-slate-400 uppercase tracking-widest text-right">Result</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {filteredAttendance.map((record) => {
                const student = students[record.studentId] || { name: "Unknown", rollNumber: "N/A" };
                const subject = subjects[record.subjectId] || { name: "N/A", code: "N/A" };
                return (
                  <tr key={record.id} className="hover:bg-indigo-50/30 transition-colors group">
                    <td className="px-10 py-8">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center font-black text-slate-400 group-hover:bg-white group-hover:text-indigo-500 transition-colors">
                          {student.name.charAt(0)}
                        </div>
                        <span className="font-bold text-slate-800 text-lg">{student.name}</span>
                      </div>
                    </td>
                    <td className="px-10 py-8">
                      <div className="flex items-center gap-2">
                        <Book className="w-4 h-4 text-slate-300" />
                        <div>
                          <div className="font-bold text-slate-700 text-sm">{subject.name}</div>
                          <div className="text-[10px] font-black text-slate-400 uppercase">{subject.code}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-10 py-8">
                      <span className="font-mono text-sm font-black text-slate-400 bg-slate-100 px-3 py-1.5 rounded-xl group-hover:bg-white transition-colors">
                        {student.rollNumber}
                      </span>
                    </td>
                    <td className="px-10 py-8">
                      <div className="flex flex-col">
                        <span className="text-slate-600 font-bold">{record.time}</span>
                        <span className="text-[10px] text-slate-400 font-black uppercase">Captured</span>
                      </div>
                    </td>
                    <td className="px-10 py-8 text-right">
                      <span className="bg-emerald-100 text-emerald-600 px-5 py-2 rounded-full text-[10px] font-black uppercase tracking-widest">
                        {record.status}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {filteredAttendance.length === 0 && (
            <div className="p-32 text-center">
              <div className="bg-slate-50 w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6">
                <ClipboardCheck className="w-10 h-10 text-slate-200" />
              </div>
              <p className="text-slate-400 font-bold text-lg">No intelligence data discovered for this query</p>
              <button 
                onClick={() => {setSearchTerm(""); setDateFilter(new Date().toISOString().split('T')[0])}}
                className="mt-4 text-indigo-600 font-black text-sm hover:underline"
              >
                Clear all filters
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AttendanceReport;
