import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Webcam from "react-webcam";
import API from "../services/api";
import { UserPlus, Camera, Save, X, Search, Mail, Fingerprint, RefreshCw, Trash2 } from "lucide-react";

const Students = () => {
  const [students, setStudents] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const webcamRef = useRef(null);
  const [capturedImage, setCapturedImage] = useState(null);
  const [editingStudentId, setEditingStudentId] = useState(null);
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    rollNumber: ""
  });

  const fetchStudents = async () => {
    try {
      const res = await API.get("/students");
      setStudents(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || user.role !== "ADMIN") {
      navigate("/");
      return;
    }
    fetchStudents();
  }, []);

  const capture = useCallback(() => {
    const imageSrc = webcamRef.current.getScreenshot();
    setCapturedImage(imageSrc);
  }, [webcamRef]);

  const handleEdit = (student) => {
    setFormData({
      name: student.name,
      email: student.email,
      rollNumber: student.rollNumber
    });
    setEditingStudentId(student.id);
    setCapturedImage(null); // Optional: if you want to allow updating the face image
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this student? This action cannot be undone.")) {
      try {
        await API.delete(`/students/${id}`, {
          headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
        });
        fetchStudents();
      } catch (err) {
        console.error(err);
        alert("Deletion failed");
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!editingStudentId && !capturedImage) {
      alert("Please capture a face image first");
      return;
    }

    setLoading(true);
    try {
      if (editingStudentId) {
        await API.put(`/students/${editingStudentId}`, {
          ...formData,
          image: capturedImage
        }, {
          headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
        });
      } else {
        await API.post("/students/register", {
          ...formData,
          image: capturedImage
        }, {
          headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
        });
      }
      setShowModal(false);
      setEditingStudentId(null);
      setCapturedImage(null);
      setFormData({ name: "", email: "", rollNumber: "" });
      fetchStudents();
    } catch (err) {
      console.error(err);
      alert(editingStudentId ? "Update failed" : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = students.filter(s => 
    s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.rollNumber.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-6xl mx-auto">
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-black text-gray-800">Student Management</h1>
            <p className="text-gray-500 mt-1">Register and manage biometric student profiles</p>
          </div>
          <button 
            onClick={() => {
              setEditingStudentId(null);
              setFormData({ name: "", email: "", rollNumber: "" });
              setCapturedImage(null);
              setShowModal(true);
            }}
            className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-3 rounded-2xl font-bold flex items-center gap-2 shadow-lg shadow-indigo-100 transition-all"
          >
            <UserPlus className="w-5 h-5" />
            Add New Student
          </button>
        </header>

        {/* Search Bar */}
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 mb-8 flex items-center gap-3">
          <Search className="w-5 h-5 text-gray-400" />
          <input 
            type="text"
            placeholder="Search by name or roll number..."
            className="flex-1 bg-transparent outline-none text-gray-700 font-medium"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        {/* Students Table */}
        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b border-gray-100">
              <tr>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Student</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Roll Number</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Contact</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredStudents.map((student) => (
                <tr key={student.id} className="hover:bg-indigo-50/30 transition-colors group">
                  <td className="px-8 py-6">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-indigo-100 flex items-center justify-center text-indigo-600 font-bold text-lg">
                        {student.name.charAt(0)}
                      </div>
                      <div>
                        <div className="font-bold text-gray-800">{student.name}</div>
                        <div className="text-xs text-indigo-500 font-semibold uppercase tracking-tight">Verified ID</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-8 py-6">
                    <span className="font-mono bg-gray-100 px-3 py-1 rounded-lg text-sm text-gray-600 font-bold">
                      {student.rollNumber}
                    </span>
                  </td>
                  <td className="px-8 py-6">
                    <div className="flex items-center gap-2 text-gray-500 text-sm">
                      <Mail className="w-4 h-4" />
                      {student.email}
                    </div>
                  </td>
                  <td className="px-8 py-6 text-right">
                    <div className="flex justify-end items-center gap-4">
                      <button 
                        onClick={() => handleEdit(student)}
                        className="text-indigo-600 hover:text-indigo-800 font-bold text-sm transition-colors"
                      >
                        Edit Profile
                      </button>
                      <button 
                        onClick={() => handleDelete(student.id)}
                        className="p-2 text-rose-500 hover:bg-rose-50 rounded-xl transition-all group/del"
                        title="Delete Student"
                      >
                        <Trash2 className="w-5 h-5 group-hover/del:scale-110" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filteredStudents.length === 0 && (
            <div className="p-20 text-center">
              <div className="bg-gray-50 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4">
                <Search className="w-8 h-8 text-gray-300" />
              </div>
              <p className="text-gray-400 font-medium">No students found matching your search</p>
            </div>
          )}
        </div>
      </div>

      {/* Registration Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-indigo-950/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <div className="p-8 border-b border-gray-100 flex justify-between items-center sticky top-0 bg-white z-10">
              <h2 className="text-2xl font-bold text-gray-800 flex items-center gap-3">
                <Fingerprint className="w-8 h-8 text-indigo-600" />
                {editingStudentId ? "Edit Student Profile" : "Register New Student"}
              </h2>
              <button onClick={() => {
                setShowModal(false);
                setEditingStudentId(null);
              }} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                <X className="w-6 h-6 text-gray-400" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-8 grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm font-bold text-gray-700">Full Name</label>
                  <input 
                    required
                    type="text" 
                    className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                    placeholder="John Doe"
                    value={formData.name}
                    onChange={e => setFormData({...formData, name: e.target.value})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-bold text-gray-700">Email Address</label>
                  <input 
                    required
                    type="email" 
                    className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                    placeholder="john@example.com"
                    value={formData.email}
                    onChange={e => setFormData({...formData, email: e.target.value})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-bold text-gray-700">Roll Number / ID</label>
                  <input 
                    required
                    type="text" 
                    className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                    placeholder="STU12345"
                    value={formData.rollNumber}
                    onChange={e => setFormData({...formData, rollNumber: e.target.value})}
                  />
                </div>
              </div>

              <div className="space-y-6">
                <label className="text-sm font-bold text-gray-700 block">Biometric Enrollment (Face Capture)</label>
                <div className="aspect-video bg-gray-900 rounded-2xl overflow-hidden relative shadow-inner ring-4 ring-gray-50">
                  {capturedImage ? (
                    <img src={capturedImage} className="w-full h-full object-cover" alt="Captured face" />
                  ) : (
                    <Webcam
                      audio={false}
                      ref={webcamRef}
                      screenshotFormat="image/jpeg"
                      className="w-full h-full object-cover"
                    />
                  )}
                  <div className="absolute inset-0 border-2 border-white/10 pointer-events-none flex items-center justify-center">
                    <div className="w-48 h-48 border-2 border-dashed border-white/30 rounded-full"></div>
                  </div>
                </div>
                <div className="flex gap-4">
                  {!capturedImage ? (
                    <button 
                      type="button"
                      onClick={capture}
                      className="flex-1 bg-gray-800 text-white py-3 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-gray-900 transition-all"
                    >
                      <Camera className="w-5 h-5" /> Capture Face
                    </button>
                  ) : (
                    <button 
                      type="button"
                      onClick={() => setCapturedImage(null)}
                      className="flex-1 bg-red-50 text-red-600 py-3 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-red-100 transition-all"
                    >
                      <RefreshCw className="w-5 h-5" /> Retake
                    </button>
                  )}
                </div>
              </div>

              <div className="md:col-span-2 pt-6 border-t border-gray-100">
                <button 
                  type="submit"
                  disabled={loading}
                  className="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black text-lg shadow-xl shadow-indigo-100 hover:bg-indigo-700 transition-all disabled:opacity-50 flex items-center justify-center gap-3"
                >
                  {loading ? <RefreshCw className="w-6 h-6 animate-spin" /> : <Save className="w-6 h-6" />}
                  {loading ? "Registering Student..." : "Complete Enrollment"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Students;
