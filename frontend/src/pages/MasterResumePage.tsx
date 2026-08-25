import { useState } from "react";
import { Alert, Button, Paper, Stack, Typography } from "@mui/material";
import { deleteMasterResume, getMasterResume, putMasterResume } from "../services/resumeService";

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(String(reader.result).split(",")[1]);
    reader.onerror = reject;
  });
}

export default function MasterResumePage(): JSX.Element {
  const [userId, setUserId] = useState("demo-user");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const data = await getMasterResume(userId);
      setMessage(`Loaded: ${data.s3Key}`);
      setError(null);
    } catch {
      setError("No master resume found.");
      setMessage(null);
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>Master Resume</Typography>
      <Stack spacing={2}>
        <input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="User ID" />
        <Button variant="outlined" component="label">
          Upload / Replace Master Resume
          <input
            hidden
            type="file"
            accept=".pdf"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              const base64 = await fileToBase64(file);
              await putMasterResume(userId, base64);
              setMessage("Master resume updated");
              setError(null);
            }}
          />
        </Button>
        <Stack direction="row" spacing={2}>
          <Button variant="contained" onClick={load}>Get</Button>
          <Button variant="outlined" color="error" onClick={async () => {
            await deleteMasterResume(userId);
            setMessage("Master resume deleted");
            setError(null);
          }}>Delete</Button>
        </Stack>
        {message && <Alert severity="success">{message}</Alert>}
        {error && <Alert severity="warning">{error}</Alert>}
      </Stack>
    </Paper>
  );
}
