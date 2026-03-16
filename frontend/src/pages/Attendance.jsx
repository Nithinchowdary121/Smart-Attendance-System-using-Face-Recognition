import { useState, useRef, useCallback, useEffect } from "react";
import Webcam from "react-webcam";
import API from "../services/api";
import { Camera, RefreshCw, CheckCircle, AlertCircle, LogOut, BookOpen, UserCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";

const Attendance = () => {
  const webcamRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [status, setStatus] = useState(null); // 'success' | 'error'
  const [subjects, setSubjects] = useState([]);
  const [selectedSubject, setSelectedSubject] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const res = await API.get("/subjects");
        setSubjects(res.data);
        if (res.data.length > 0) setSelectedSubject(res.data[0].id);
      } catch (err) {
        console.error("Failed to fetch subjects");
      }
    };
    fetchSubjects();
  }, []);

  const capture = useCallback(async () => {
    if (!selectedSubject) {
      setMessage("Please select a subject first.");
      setStatus("error");
      return;
    }

    const imageSrc = webcamRef.current.getScreenshot();
    if (!imageSrc) return;

    setLoading(true);
    setMessage("Analyzing biometric features...");
    setStatus(null);

    try {
      const response = await API.post("/attendance/mark", { 
        image: imageSrc,
        subjectId: selectedSubject
      });
      setMessage(response.data);
      if (response.data.toLowerCase().includes("marked")) {
        setStatus("success");
      } else {
        setStatus("error");
      }
    } catch (err) {
      setMessage("Biometric verification failed. Please try again.");
      setStatus("error");
    } finally {
      setLoading(false);
    }
  }, [webcamRef, selectedSubject]);

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <nav className="bg-white/80 backdrop-blur-md sticky top-0 z-30 border-b border-slate-200 px-8 py-5 flex justify-between items-center">
        <h1 className="text-2xl font-black text-indigo-600 flex items-center gap-3">
          <div className="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-indigo-100">
            <UserCheck className="w-6 h-6" />
          </div>
          Attendance Portal
        </h1>
        <button 
          onClick={handleLogout}
          className="flex items-center gap-2 text-slate-400 hover:text-rose-500 transition-colors font-bold text-sm uppercase tracking-widest"
        >
          <LogOut className="w-5 h-5" />
          Terminate Session
        </button>
      </nav>

      <main className="max-w-5xl mx-auto p-8 lg:p-12">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          
          {/* Camera Section */}
          <div className="lg:col-span-7 space-y-8">
            <div className="bg-white rounded-[2.5rem] shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-8 text-center border-b border-slate-50">
                <h2 className="text-2xl font-black text-slate-800">Biometric Verification</h2>
                <p className="text-slate-400 mt-1 font-medium">Real-time facial recognition engine active</p>
              </div>

              <div className="p-10">
                <div className="relative aspect-video rounded-3xl overflow-hidden bg-slate-900 shadow-2xl ring-8 ring-slate-50">
                  <Webcam
                    audio={false}
                    ref={webcamRef}
                    screenshotFormat="image/jpeg"
                    className="w-full h-full object-cover scale-x-[-1]"
                  />
                  <div className="absolute inset-0 border-2 border-white/10 pointer-events-none flex items-center justify-center">
                    <div className="w-64 h-80 border-4 border-dashed border-indigo-400/50 rounded-[4rem] animate-pulse"></div>
                    <div className="absolute top-4 left-4 flex items-center gap-2 bg-slate-900/50 backdrop-blur-md px-3 py-1.5 rounded-lg border border-white/10">
                      <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></div>
                      <span className="text-[10px] font-black text-white uppercase tracking-widest">Live Cam</span>
                    </div>
                  </div>
                </div>
                <p className="text-center text-slate-400 text-xs mt-4 font-bold">
                  Tip: Position your face inside the dashed frame for better recognition.
                </p>

                <div className="mt-10 flex flex-col items-center gap-6">
                  {message && (
                    <div className={`w-full flex items-center gap-4 px-8 py-5 rounded-2xl border ${
                      status === 'success' ? 'bg-emerald-50 border-emerald-100 text-emerald-700' : 
                      status === 'error' ? 'bg-rose-50 border-rose-100 text-rose-700' : 
                      'bg-indigo-50 border-indigo-100 text-indigo-700'
                    } transition-all animate-in fade-in slide-in-from-top-4`}>
                      {status === 'success' ? <CheckCircle className="w-6 h-6 shrink-0" /> : 
                       status === 'error' ? <AlertCircle className="w-6 h-6 shrink-0" /> : 
                       <RefreshCw className="w-6 h-6 shrink-0 animate-spin" />}
                      <span className="font-bold text-lg leading-tight">{message}</span>
                    </div>
                  )}

                  <button
                    onClick={capture}
                    disabled={loading || !selectedSubject}
                    className="w-full group relative flex items-center justify-center gap-4 bg-indigo-600 hover:bg-indigo-700 text-white px-10 py-6 rounded-[2rem] font-black text-xl shadow-2xl shadow-indigo-100 transition-all transform hover:-translate-y-1 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                  >
                    {loading ? (
                      <RefreshCw className="w-7 h-7 animate-spin" />
                    ) : (
                      <Camera className="w-7 h-7 group-hover:rotate-12 transition-transform" />
                    )}
                    {loading ? "PROCESSING..." : "VERIFY & MARK"}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Settings Section */}
          <div className="lg:col-span-5 space-y-8">
            <div className="bg-white rounded-[2.5rem] p-10 shadow-sm border border-slate-100">
              <h3 className="text-xl font-black text-slate-800 mb-8 flex items-center gap-3">
                <BookOpen className="w-6 h-6 text-indigo-500" />
                Session Selection
              </h3>
              
              <div className="space-y-6">
                <div className="space-y-3">
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Select Current Subject</label>
                  <div className="grid grid-cols-1 gap-3">
                    {subjects.map((subject) => (
                      <button
                        key={subject.id}
                        onClick={() => setSelectedSubject(subject.id)}
                        className={`p-5 rounded-2xl border-2 transition-all text-left flex items-center justify-between group ${
                          selectedSubject === subject.id 
                          ? 'border-indigo-600 bg-indigo-50/50' 
                          : 'border-slate-50 bg-slate-50/50 hover:border-slate-200'
                        }`}
                      >
                        <div>
                          <div className={`font-black text-sm ${selectedSubject === subject.id ? 'text-indigo-600' : 'text-slate-600'}`}>
                            {subject.name}
                          </div>
                          <div className="text-[10px] font-bold text-slate-400 mt-0.5">{subject.code}</div>
                        </div>
                        {selectedSubject === subject.id && (
                          <div className="w-6 h-6 bg-indigo-600 rounded-full flex items-center justify-center">
                            <CheckCircle className="w-4 h-4 text-white" />
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-indigo-600 rounded-[2.5rem] p-10 text-white shadow-xl shadow-indigo-100 relative overflow-hidden">
               <div className="relative z-10">
                <h3 className="text-lg font-black mb-4">Biometric Protocol</h3>
                <ul className="space-y-4">
                  {[
                    "Position face within the central guide",
                    "Ensure adequate environmental lighting",
                    "Maintain a neutral facial expression",
                    "System timeout after 3 failed attempts"
                  ].map((text, i) => (
                    <li key={i} className="flex gap-3 text-sm font-bold text-indigo-100 leading-tight">
                      <div className="w-1.5 h-1.5 bg-white rounded-full mt-1.5 shrink-0"></div>
                      {text}
                    </li>
                  ))}
                </ul>
               </div>
               <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
};

export default Attendance;
