from apps.worker.job_runner import InProcessJobRunner
from packages.core.checkpoints.service import SqlAlchemyCheckpointService
from packages.core.db.session import init_database, reset_database
from packages.core.jobs.service import SqlAlchemyJobService
from packages.core.projects.service import ProjectInitAppService
from packages.core.sessions.service import SqlAlchemySessionService
from packages.core.workflow.orchestrator import WorkflowOrchestrator
from packages.scanner.orchestrator import ScannerOrchestrator

init_database()

_job_service = SqlAlchemyJobService()
_scanner = ScannerOrchestrator()
_job_runner = InProcessJobRunner()
_project_service = ProjectInitAppService(_job_service, _scanner, _job_runner)
_session_service = SqlAlchemySessionService()
_checkpoint_service = SqlAlchemyCheckpointService()
_workflow = WorkflowOrchestrator(_session_service, _checkpoint_service)


def get_project_service() -> ProjectInitAppService:
    return _project_service


def get_workflow() -> WorkflowOrchestrator:
    return _workflow


def reset_skeleton_state() -> None:
    _job_runner.wait_for_idle(timeout=2.0)
    reset_database()


def shutdown_background_workers() -> None:
    _job_runner.wait_for_idle(timeout=2.0)
    _job_runner.shutdown(wait_for_tasks=True)


