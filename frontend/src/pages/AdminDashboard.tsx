import { useEffect, useState } from "react";
import { Alert, Box, Button, Paper, Stack, Typography } from "@mui/material";
import { getAnalytics } from "../services/resumeService";

interface Metrics {
  resume_tailored: number;
  website_visit: number;
}

export default function AdminDashboard(): JSX.Element {
  const [metrics, setMetrics] = useState<Metrics>({ resume_tailored: 0, website_visit: 0 });
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      setError(null);
      setMetrics(await getAnalytics());
    } catch {
      setError("Failed to load analytics");
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <Paper sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5">Admin Dashboard</Typography>
        <Button variant="outlined" onClick={load}>Refresh</Button>
      </Stack>
      {error && <Alert severity="error">{error}</Alert>}
      <Box>
        <Typography>Resumes Tailored: {metrics.resume_tailored}</Typography>
        <Typography>Website Visits: {metrics.website_visit}</Typography>
      </Box>
    </Paper>
  );
}
