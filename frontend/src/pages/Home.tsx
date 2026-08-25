import { FormEvent, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { sendResumeEmail, tailorResume } from "../services/resumeService";

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(String(reader.result).split(",")[1]);
    reader.onerror = reject;
  });
}

export default function Home(): JSX.Element {
  const [resumeFile, setResumeFile] = useState<File | null>(null);
  const [jobDescription, setJobDescription] = useState("");
  const [includeCoverLetter, setIncludeCoverLetter] = useState(true);
  const [email, setEmail] = useState("");
  const [pdfBase64, setPdfBase64] = useState<string | null>(null);
  const [coverLetterBase64, setCoverLetterBase64] = useState<string | null>(null);
  const [atsScore, setAtsScore] = useState<number | null>(null);
  const [requirements, setRequirements] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [emailStatus, setEmailStatus] = useState<string | null>(null);

  const previewSource = useMemo(() => (pdfBase64 ? `data:application/pdf;base64,${pdfBase64}` : ""), [pdfBase64]);

  const onTailor = async (e: FormEvent) => {
    e.preventDefault();
    if (!resumeFile) {
      setError("Please upload a PDF resume.");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const resumePdfBase64 = await fileToBase64(resumeFile);
      const { result } = await tailorResume({ resumePdfBase64, jobDescription, includeCoverLetter });
      setPdfBase64(result.pdfBase64);
      setCoverLetterBase64(result.coverLetterPdfBase64 ?? null);
      setAtsScore(result.atsScore ?? null);
      setRequirements(result.extractedRequirements ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to tailor resume");
    } finally {
      setLoading(false);
    }
  };

  const onSendEmail = async () => {
    if (!email) return;
    setLoading(true);
    setEmailStatus(null);
    try {
      await sendResumeEmail({ email, pdfBase64, coverLetterPdfBase64: coverLetterBase64 });
      setEmailStatus("success");
    } catch {
      setEmailStatus("error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>Tailor Resume</Typography>
        <Box component="form" onSubmit={onTailor}>
          <Stack spacing={2}>
            <Button variant="outlined" component="label">
              {resumeFile ? resumeFile.name : "Upload Master Resume (PDF)"}
              <input type="file" hidden accept=".pdf" onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)} />
            </Button>
            <TextField
              label="Job Description"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              multiline
              minRows={8}
              required
            />
            <FormControlLabel
              control={<Checkbox checked={includeCoverLetter} onChange={(e) => setIncludeCoverLetter(e.target.checked)} />}
              label="Include Cover Letter"
            />
            <Button type="submit" variant="contained" disabled={loading}>Generate</Button>
          </Stack>
        </Box>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      {pdfBase64 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle1">Result</Typography>
          {atsScore !== null && <Typography>ATS Score: {atsScore.toFixed(1)}</Typography>}
          {requirements.length > 0 && <Typography>Top requirements: {requirements.join(", ")}</Typography>}
          <Box sx={{ my: 2 }}>
            <iframe src={previewSource} title="resume" style={{ width: "100%", height: 480, border: "1px solid #ddd" }} />
          </Box>
          <Stack direction="row" spacing={2}>
            <TextField label="Email" value={email} onChange={(e) => setEmail(e.target.value)} size="small" />
            <Button variant="contained" onClick={onSendEmail} disabled={loading}>Send</Button>
          </Stack>
          {emailStatus === "success" && <Alert sx={{ mt: 2 }} severity="success">Email sent</Alert>}
          {emailStatus === "error" && <Alert sx={{ mt: 2 }} severity="error">Email failed</Alert>}
        </Paper>
      )}
    </Stack>
  );
}
