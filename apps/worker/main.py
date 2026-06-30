from apps.worker.job_runner import InProcessJobRunner
from packages.core.jobs.service import SqlAlchemyJobService


class WorkerApp:
    """Placeholder worker entrypoint for future queue-based execution."""

    def __init__(self, job_service: SqlAlchemyJobService, job_runner: InProcessJobRunner) -> None:
        self.job_service = job_service
        self.job_runner = job_runner

    def run(self) -> None:
        print("AIPA worker skeleton ready. Queue integration pending.")

