#!/usr/bin/env python3
"""Enforce source-level architectural boundaries for every production module."""

import ast
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def verify():
    violations = []
    java_count = 0
    for module in ("core", "api", "webapp"):
        for path in (ROOT / module / "src/main/java").rglob("*.java"):
            java_count += 1
            source = path.read_text()
            imports = re.findall(r"^import (?:static )?([^;]+);", source, re.MULTILINE)
            relative = path.relative_to(ROOT)
            in_dao = "dao" in path.parts
            in_persistence_config = (
                "repository" in path.parts and "config" in path.parts
            )
            for dependency in imports:
                if module == "core" and dependency.startswith(
                    (
                        "jakarta.servlet.",
                        "org.springframework.web.",
                        "org.springframework.http.",
                    )
                ):
                    violations.append(
                        f"{relative}: business JAR depends on HTTP: {dependency}"
                    )
                direct_persistence = dependency.startswith(
                    "org.springframework.jdbc."
                ) or dependency in {
                    "jakarta.persistence.EntityManager",
                    "javax.sql.DataSource",
                    "org.springframework.transaction.support.TransactionTemplate",
                    "org.springframework.transaction.PlatformTransactionManager",
                }
                if direct_persistence and not (in_dao or in_persistence_config):
                    violations.append(
                        f"{relative}: direct persistence outside DAO/config: {dependency}"
                    )
                if (
                    module != "core"
                    and ".core." in dependency
                    and any(part in dependency for part in (".repository.", ".domain."))
                ):
                    violations.append(
                        f"{relative}: transport depends on persistence/domain: {dependency}"
                    )
                if module == "core" and dependency.startswith(
                    ("io.agentengine.api.", "io.agentengine.webapp.")
                ):
                    violations.append(
                        f"{relative}: reversed module dependency: {dependency}"
                    )
            if module != "core" and re.search(
                r"\.(?:findByClientId|findByToken)\s*\(", source
            ):
                violations.append(
                    f"{relative}: transport reads OAuth persistence directly"
                )
            if module != "core" and re.search(
                r"@Transactional\b|\b(?:SELECT|INSERT INTO|UPDATE .* SET|DELETE FROM)\b",
                source,
            ):
                violations.append(f"{relative}: transport defines transaction or SQL")
            if path.name.endswith("Repository.java") and not in_dao:
                if not re.search(r"extends JpaRepository<", source):
                    violations.append(
                        f"{relative}: repository must directly extend JpaRepository"
                    )
                if re.search(r"\b(?:default|kind\s*\()", source):
                    violations.append(
                        f"{relative}: repository contains dispatch/business behavior"
                    )
            if (
                module == "core"
                and "domain" in path.parts
                and re.search(r"@(ManyToOne|OneToMany|OneToOne|ManyToMany)\b", source)
            ):
                violations.append(f"{relative}: entity relationship must use scalar ID")
            if "impl" in path.parts:
                violations.append(
                    f"{relative}: implementation must share its interface folder"
                )
            record_name = re.search(r"\brecord\s+(\w+)", source)
            if record_name and path.stem.endswith("DTO"):
                violations.append(f"{relative}: Java records must end in Record, not DTO")
            if "record" in path.parts and not path.stem.endswith("Record"):
                violations.append(f"{relative}: record name must end in Record")
            if path.stem == "ResourceKind":
                body = source.split("{", 1)[1].rsplit("}", 1)[0]
                if not re.fullmatch(r"[\sA-Z_,]+", body):
                    violations.append(f"{relative}: resource enum must only enumerate values")
            if "dto" in path.parts and not path.stem.endswith("DTO"):
                violations.append(f"{relative}: DTO name must end in DTO")
    python_count = 0
    runtime = ROOT / "coordinator/agent_engine_local"
    for path in runtime.rglob("*.py"):
        python_count += 1
        tree = ast.parse(path.read_text(), filename=str(path))
        for node in ast.walk(tree):
            if (
                isinstance(node, ast.Import)
                and any(alias.name == "sqlite3" for alias in node.names)
                and "repository" not in path.parts
            ):
                violations.append(
                    f"{path.relative_to(ROOT)}: SQLite outside persistence"
                )
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                if (
                    node.func.attr
                    in {"execute", "executemany", "executescript", "commit", "rollback"}
                    and "repository" not in path.parts
                ):
                    # Native model execution is an integration API, not SQL execution.
                    if not (
                        node.func.attr == "execute"
                        and isinstance(node.func.value, ast.Call)
                        and isinstance(node.func.value.func, ast.Name)
                        and node.func.value.func.id == "CodexClient"
                    ):
                        violations.append(
                            f"{path.relative_to(ROOT)}:{node.lineno}: persistence call outside repository/DAO"
                        )
    if violations:
        raise AssertionError("\n".join(violations))
    print(
        f"PASS: architectural boundaries across {java_count} Java and {python_count} Python source files"
    )


if __name__ == "__main__":
    verify()
