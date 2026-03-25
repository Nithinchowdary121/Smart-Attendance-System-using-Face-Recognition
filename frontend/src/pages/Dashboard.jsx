import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import API from "../services/api";
import { Users, ClipboardCheck, BarChart3, PlusCircle, LogOut, ChevronRight, TrendingUp, Calendar, Clock } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';

const Dashboard = () => {
  const [stats, setStats] = useState({ students: 0, attendanceToday: 0 });
  const [chartData, setChartData] = useState([]);
  const [recentAttendance, setRecentAttendance] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || user.role !== "ADMIN") {
      navigate("/");
      return;
    }

    const fetchData = async () => {
      try {
        const [studentsRes, attendanceRes] = await Promise.all([
          API.get("/students"),
          API.get("/attendance/report")
        ]);
        
        const today = new Date().toISOString().split('T')[0];
        const todayAttendance = attendanceRes.data.filter(a => a.date === today).length;

        setStats({
          students: studentsRes.data.length,
          attendanceToday: todayAttendance
        });

        // Group attendance by date for charts
        const grouped = attendanceRes.data.reduce((acc, curr) => {
          acc[curr.date] = (acc[curr.date] || 0) + 1;
          return acc;
        }, {});

        const last7Days = Array.from({ length: 7 }, (_, i) => {
          const d = new Date();
          d.setDate(d.getDate() - (6 - i));
          const dateStr = d.toISOString().split('T')[0];
          return {
            name: new Date(dateStr).toLocaleDateString('en-US', { weekday: 'short' }),
            count: grouped[dateStr] || 0
          };
        });
        setChartData(last7Days);

        // Get student names for recent attendance
        const stuMap = {};
        studentsRes.data.forEach(s => stuMap[s.id] = s.name);
        
        const recent = attendanceRes.data
          .slice(-5)
          .reverse()
          .map(a => ({
            ...a,
            studentName: stuMap[a.studentId] || "Unknown Student"
          }));
        setRecentAttendance(recent);

      } catch (err) {
        console.error(err);
      }
    };
    fetchData();
  }, []);

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  const cards = [
    { title: "Total Students", value: stats.students, icon: Users, color: "bg-blue-600", shadow: "shadow-blue-100", link: "/students" },
    { title: "Today's Attendance", value: stats.attendanceToday, icon: ClipboardCheck, color: "bg-emerald-600", shadow: "shadow-emerald-100", link: "/attendance-report" },
    { title: "Attendance Rate", value: `${stats.students > 0 ? Math.round((stats.attendanceToday / stats.students) * 100) : 0}%`, icon: BarChart3, color: "bg-indigo-600", shadow: "shadow-indigo-100", link: "/attendance-report" }
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Sidebar */}
      <aside className="w-72 bg-slate-900 text-white hidden lg:flex flex-col sticky top-0 h-screen">
        <div className="p-8 border-b border-slate-800">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-indigo-500 rounded-xl flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-xl font-black tracking-tight">Smart Attend</h1>
          </div>
          <p className="text-slate-400 text-[10px] uppercase font-bold tracking-widest">Biometric Management</p>
        </div>
        
        <nav className="flex-1 px-4 py-6 space-y-2">
          <Link to="/dashboard" className="flex items-center gap-3 px-4 py-3 bg-indigo-600 text-white rounded-xl font-bold transition-all shadow-lg shadow-indigo-900/20">
            <BarChart3 className="w-5 h-5" /> Dashboard
          </Link>
          <Link to="/students" className="flex items-center gap-3 px-4 py-3 text-slate-400 hover:text-white hover:bg-slate-800/50 rounded-xl transition-all font-semibold group">
            <Users className="w-5 h-5 group-hover:scale-110 transition-transform" /> Students
          </Link>
          <Link to="/attendance-report" className="flex items-center gap-3 px-4 py-3 text-slate-400 hover:text-white hover:bg-slate-800/50 rounded-xl transition-all font-semibold group">
            <ClipboardCheck className="w-5 h-5 group-hover:scale-110 transition-transform" /> Reports
          </Link>
        </nav>

        <div className="p-6 border-t border-slate-800 bg-slate-900/50">
          <button 
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-rose-500/10 text-rose-500 hover:bg-rose-500 hover:text-white rounded-xl transition-all font-bold"
          >
            <LogOut className="w-5 h-5" /> Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-x-hidden">
        <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 px-8 py-5 flex justify-between items-center sticky top-0 z-20">
          <div>
            <h2 className="text-2xl font-black text-slate-800">Administrator Console</h2>
            <div className="flex items-center gap-4 mt-1">
               <span className="flex items-center gap-1.5 text-slate-400 text-sm font-medium">
                <Calendar className="w-4 h-4" /> {new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
              </span>
              <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
              <span className="flex items-center gap-1.5 text-emerald-500 text-sm font-bold uppercase tracking-wider">
                System Active
              </span>
            </div>
          </div>
          <Link to="/students" className="flex items-center gap-2 bg-slate-900 hover:bg-indigo-600 text-white px-6 py-3 rounded-xl font-bold transition-all shadow-xl shadow-slate-200 group">
            <PlusCircle className="w-5 h-5 group-hover:rotate-90 transition-transform" />
            New Enrollment
          </Link>
        </header>

        <div className="p-8 space-y-8">
          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {cards.map((card, i) => (
              <div key={i} className="bg-white rounded-[2rem] p-8 shadow-sm border border-slate-100 relative overflow-hidden group hover:border-indigo-100 transition-colors">
                <div className={`${card.color} w-14 h-14 rounded-2xl flex items-center justify-center text-white mb-6 transform group-hover:scale-110 transition-transform shadow-lg ${card.shadow}`}>
                  <card.icon className="w-7 h-7" />
                </div>
                <p className="text-slate-400 font-bold uppercase text-[10px] tracking-widest">{card.title}</p>
                <h3 className="text-4xl font-black text-slate-800 mt-1">{card.value}</h3>
                <Link to={card.link} className="mt-6 flex items-center text-sm font-black text-indigo-600 hover:gap-2 transition-all">
                  Analyze Data <ChevronRight className="w-4 h-4" />
                </Link>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Chart Area */}
            <div className="lg:col-span-2 bg-white rounded-[2.5rem] p-10 shadow-sm border border-slate-100">
              <div className="flex justify-between items-center mb-10">
                <div>
                  <h3 className="text-xl font-black text-slate-800">Attendance Analytics</h3>
                  <p className="text-slate-400 text-sm font-medium mt-1">Weekly participation trends</p>
                </div>
                <div className="flex gap-2">
                  <span className="w-3 h-3 bg-indigo-500 rounded-full"></span>
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-tighter">Students Present</span>
                </div>
              </div>
              <div className="h-80 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <defs>
                      <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.1}/>
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis 
                      dataKey="name" 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 700}}
                      dy={10}
                    />
                    <YAxis 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 700}}
                    />
                    <Tooltip 
                      contentStyle={{borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)'}}
                    />
                    <Area 
                      type="monotone" 
                      dataKey="count" 
                      stroke="#6366f1" 
                      strokeWidth={4}
                      fillOpacity={1} 
                      fill="url(#colorCount)" 
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Recent Activity */}
            <div className="bg-white rounded-[2.5rem] p-10 shadow-sm border border-slate-100">
              <h3 className="text-xl font-black text-slate-800 mb-8 flex items-center justify-between">
                Live Feed
                <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
              </h3>
              <div className="space-y-8">
                {recentAttendance.map((log, i) => (
                  <div key={i} className="flex gap-4 group">
                    <div className="w-12 h-12 rounded-2xl bg-slate-50 flex items-center justify-center text-slate-400 font-black text-lg group-hover:bg-indigo-50 group-hover:text-indigo-500 transition-colors">
                      {log.studentName.charAt(0)}
                    </div>
                    <div className="flex-1">
                      <div className="flex justify-between items-start">
                        <h4 className="font-bold text-slate-800 leading-tight">{log.studentName}</h4>
                        <span className="text-[10px] font-black bg-emerald-100 text-emerald-600 px-2 py-0.5 rounded-full">PRESENT</span>
                      </div>
                      <div className="flex items-center gap-3 mt-1">
                        <span className="flex items-center gap-1 text-[11px] font-bold text-slate-400">
                          <Clock className="w-3 h-3" /> {log.time}
                        </span>
                        <span className="w-1 h-1 bg-slate-200 rounded-full"></span>
                        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-tighter">Verified</span>
                      </div>
                    </div>
                  </div>
                ))}
                {recentAttendance.length === 0 && (
                  <div className="text-center py-10">
                    <p className="text-slate-400 font-bold text-sm">Waiting for logs...</p>
                  </div>
                )}
              </div>
              <Link to="/attendance-report" className="mt-10 w-full flex items-center justify-center gap-2 py-4 border-2 border-slate-100 rounded-2xl text-slate-500 font-black text-sm hover:bg-slate-50 transition-all">
                View Full Logs
              </Link>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
