import axios from "axios";

const getBaseURL = () => {
  // Use the environment variable if available
  let url = import.meta.env.VITE_API_BASE_URL;
  
  // Detection logic for production vs development
  const isProduction = window.location.hostname !== "localhost" && !window.location.hostname.includes("127.0.0.1");

  if (!url) {
    if (isProduction) {
      // Set the provided Render URL as default for production if environment variable is missing
      return "https://smart-attendance-system-using-face-pzj6.onrender.com/api";
    }
    url = "http://localhost:8080/api";
  }

  // Automatically append /api if missing, but only if it's a full URL
  if (url.startsWith("http") && !url.endsWith("/api") && !url.includes("/api/")) {
    url = url.endsWith("/") ? `${url}api` : `${url}/api`;
  }
  
  return url;
};

const API = axios.create({
  baseURL: getBaseURL()
});

// Add a request interceptor to include JWT token
API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Add a response interceptor for better error handling
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      console.error("Network Error or CORS issue:", error);
    }
    return Promise.reject(error);
  }
);

export default API;
