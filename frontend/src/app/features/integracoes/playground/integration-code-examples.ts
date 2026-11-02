import { Component, signal } from '@angular/core';

interface CodeSample {
  id: 'curl' | 'java' | 'python' | 'javascript';
  label: string;
  code: string;
}

@Component({
  selector: 'app-integration-code-examples',
  standalone: true,
  templateUrl: './integration-code-examples.html',
  styleUrl: './integration-code-examples.css',
})
export class IntegrationCodeExamples {
  readonly activeSample = signal<CodeSample['id']>('curl');
  readonly copiedSample = signal<CodeSample['id'] | null>(null);

  readonly samples: CodeSample[] = [
    {
      id: 'curl',
      label: 'cURL',
      code: `curl --fail-with-body \\
  -H "Authorization: Bearer SUA_API_KEY" \\
  -F "arquivo=@CAMINHO_DO_DOCUMENTO;type=application/pdf" \\
  URL_DA_API/api/v1/public/analises`,
    },
    {
      id: 'java',
      label: 'Java 21',
      code: `import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

var apiUrl = "URL_DA_API/api/v1/public/analises";
var apiKey = "SUA_API_KEY";
var document = Path.of("CAMINHO_DO_DOCUMENTO");
var boundary = "----DocumentShield" + UUID.randomUUID();
var prefix = ("--" + boundary + "\\r\\n"
    + "Content-Disposition: form-data; name=\\"arquivo\\"; filename=\\"documento.pdf\\"\\r\\n"
    + "Content-Type: application/pdf\\r\\n\\r\\n").getBytes(StandardCharsets.UTF_8);
var suffix = ("\\r\\n--" + boundary + "--\\r\\n").getBytes(StandardCharsets.UTF_8);

var body = HttpRequest.BodyPublishers.concat(
    HttpRequest.BodyPublishers.ofByteArray(prefix),
    HttpRequest.BodyPublishers.ofFile(document),
    HttpRequest.BodyPublishers.ofByteArray(suffix));
var request = HttpRequest.newBuilder(URI.create(apiUrl))
    .header("Authorization", "Bearer " + apiKey)
    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
    .POST(body)
    .build();

var response = HttpClient.newHttpClient()
    .send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());`,
    },
    {
      id: 'python',
      label: 'Python',
      code: `import requests

with open("CAMINHO_DO_DOCUMENTO", "rb") as document:
    response = requests.post(
        "URL_DA_API/api/v1/public/analises",
        headers={"Authorization": "Bearer SUA_API_KEY"},
        files={"arquivo": ("documento.pdf", document, "application/pdf")},
        timeout=35,
    )

response.raise_for_status()
print(response.json())`,
    },
    {
      id: 'javascript',
      label: 'JavaScript',
      code: `const URL_DA_API = "URL_DA_API";
const SUA_API_KEY = "SUA_API_KEY";
// Selecione CAMINHO_DO_DOCUMENTO no input de arquivo do navegador.
const arquivo = document.querySelector("#arquivo").files[0];
const formData = new FormData();
formData.append("arquivo", arquivo, arquivo.name);

const response = await fetch(
  URL_DA_API + "/api/v1/public/analises",
  {
    method: "POST",
    headers: { Authorization: "Bearer " + SUA_API_KEY },
    body: formData,
  },
);

if (!response.ok) throw new Error(await response.text());
console.log(await response.json());`,
    },
  ];

  get selectedSample(): CodeSample {
    return this.samples.find((sample) => sample.id === this.activeSample()) ?? this.samples[0];
  }

  selectSample(sample: CodeSample['id']): void {
    this.activeSample.set(sample);
    this.copiedSample.set(null);
  }

  async copySample(): Promise<void> {
    const sample = this.selectedSample;
    try {
      await navigator.clipboard.writeText(sample.code);
      this.copiedSample.set(sample.id);
      window.setTimeout(() => this.copiedSample.set(null), 2000);
    } catch {
      this.copiedSample.set(null);
    }
  }
}
