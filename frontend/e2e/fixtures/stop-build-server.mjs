// Close the test-owned server explicitly; avoids Windows process-tree termination delays.
export default async function teardown() {
  await fetch("http://127.0.0.1:15173/__test_shutdown", { method: "POST" });
}
