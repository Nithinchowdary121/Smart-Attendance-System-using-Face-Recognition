import axios from "axios";

const getBaseURL = () => {
  let url = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  // Automatically append /api if missing
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
